// Migrate.js - MongoDB migration to adjust Special Digivolve & Assembly fields
// Targets the singular collection: card

const cursor = db.card.find({
  $or: [
    { specialDigivolve: { $ne: null } },
    { assemblyEffect: { $ne: null } },
    { mainEffect: { $regex: /Assembly/i } }
  ]
});

let updatedCount = 0;

cursor.forEach(card => {
  let updated = false;
  let updateFields = {};

  // 1. Process specialDigivolve: Format "Digivolve:" to "[Digivolve]"
  if (card.specialDigivolve) {
    let originalDigivolve = card.specialDigivolve;
    let formattedDigivolve = originalDigivolve.trim();

    if (/^Digivolve:/i.test(formattedDigivolve)) {
      formattedDigivolve = formattedDigivolve.replace(/^Digivolve:\s*/i, "[Digivolve] ");
      updated = true;
    } else if (/^Digivolve\s/i.test(formattedDigivolve)) {
      formattedDigivolve = formattedDigivolve.replace(/^Digivolve\s+/i, "[Digivolve] ");
      updated = true;
    }

    if (updated) {
      // Normalize any consecutive white spaces
      formattedDigivolve = formattedDigivolve.replace(/\s+/g, ' ').trim();
      updateFields.specialDigivolve = formattedDigivolve;
    }
  }

  // 2. Process Assembly: Extract from mainEffect and correct bracket style
  let main = card.mainEffect || "";
  let assembly = card.assemblyEffect || null;
  let assemblyExtracted = false;

  // Regex to detect any form of Assembly in the main effect (covers <, ＜, and [ )
  const assemblyRegex = /(?:^|\n)(?:[＜<\[])(Assembly\s*[-–\d\w\s]+)(?:[＞>\]])([^\n]*)/i;
  let match = main.match(assemblyRegex);

  if (match) {
    let assemblyTag = match[1].trim(); // e.g. "Assembly -2"
    let assemblyReqs = match[2].trim(); // e.g. "2 Digimon cards with the [CT] trait"
    
    assembly = `[${assemblyTag}] ${assemblyReqs}`;
    main = main.replace(assemblyRegex, "").trim();
    
    updateFields.mainEffect = main || null;
    assemblyExtracted = true;
    updated = true;
  }

  // 3. Clean up any existing assembly effects to use square brackets instead of angle brackets
  if (assembly) {
    let originalAssembly = assembly;
    let cleanedAssembly = assembly.replace(/[＜<]/g, "[").replace(/[＞>]/g, "]").trim();
    cleanedAssembly = cleanedAssembly.replace(/\s+/g, ' ');
    
    if (cleanedAssembly !== originalAssembly || assemblyExtracted) {
      updateFields.assemblyEffect = cleanedAssembly;
      updated = true;
    }
  }

  // Write changes to database if any update is required
  if (updated) {
    db.card.updateOne({ _id: card._id }, { $set: updateFields });
    print(`Updated Card: ${card.cardNumber} (${card.name})`);
    if (updateFields.specialDigivolve) {
      print(`  -> Special Digivolve: "${updateFields.specialDigivolve}"`);
    }
    if (updateFields.assemblyEffect) {
      print(`  -> Assembly Effect:  "${updateFields.assemblyEffect}"`);
    }
    updatedCount++;
  }
});

print(`\nMigration completed. Successfully processed and updated ${updatedCount} cards.`);