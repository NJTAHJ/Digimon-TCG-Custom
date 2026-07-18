// Updates.js - Targeted hotfix for DigiXros and Assembly formatting

// 1. Fix Assembly on BB1-016 (MetalGreymon)
db.card.updateOne(
  { uniqueCardNumber: "BB1-016" },
  { $set: { assemblyEffect: "[Assembly\u00a0-2] 1 Level 4 or lower Digimon card with the [Nerds] trait" } }
);

// 2. Fix DigiXros on BB1-019 (MagnaKidmon)
db.card.updateOne(
  { uniqueCardNumber: "BB1-019" },
  { $set: { digiXros: "[DigiXros\u00a0-2] 2 Lv.5 or lower Digimon cards with the [IADA] trait and different levels" } }
);

// 3. Fix Assembly on BB1-033 (WereGarurumon)
db.card.updateOne(
  { uniqueCardNumber: "BB1-033" },
  { $set: { assemblyEffect: "[Assembly\u00a0-2] Level 4 or lower Digimon card with the [Nerds] trait" } }
);

// 4. Fix Assembly on BB1-034 (ShineGreymon)
db.card.updateOne(
  { uniqueCardNumber: "BB1-034" },
  { $set: { assemblyEffect: "[Assembly\u00a0-5] [Blue] [Greymon] x [Laura Loath]" } }
);

// 5. Fix Assembly on BB1-068 (Cerberumon: Werewolf Mode)
db.card.updateOne(
  { uniqueCardNumber: "BB1-068" },
  { $set: { assemblyEffect: "[Assembly\u00a0-2] 2 Digimon cards with the [CT] trait" } }
);

// 6. Fix Assembly on BB1-069 (MetalGreymon)
db.card.updateOne(
  { uniqueCardNumber: "BB1-069" },
  { $set: { assemblyEffect: "[Assembly\u00a0-2] 1 Option card with the [Three Musketeers] trait" } }
);

// 7. Fix Assembly on AD1-025_P2 (Omnimon Alt Art)
db.card.updateOne(
  { uniqueCardNumber: "AD1-025_P2" },
  { $set: { assemblyEffect: "[Assembly\u00a0-6] [WarGreymon] x [MetalGarurumon] " } }
);

// 8. Fix DigiXros on AD1-006 (Shoutmon X7)
db.card.updateOne(
  { uniqueCardNumber: "AD1-006" },
  { $set: { digiXros: "[DigiXros\u00a0-2] [OmniShoutmon] x [ZeigGreymon] x [Ballistamon] x [Dorulumon] x [Starmons] x [Sparrowmon] " } }
);

print("DigiXros and Assembly formatting successfully hotfixed in the database.");