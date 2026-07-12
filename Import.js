// 1. Clear existing database entries for these cards to avoid duplicates
db.card.deleteMany({ 
  uniqueCardNumber: { 
    $in: [
      "BB1-011", "BB1-012", "BB1-027", "BB1-028", "BB1-029", "BB1-031", 
      "BB1-041", "BB1-043", "BB1-054", "BB1-055", "BB1-057", "BB1-065", 
      "BB1-066", "BB1-067", "BB1-076", "BB1-077"
    ] 
  } 
});

// 2. Insert the Champion and Ultimate Digimon
db.card.insertMany([
  {
    "uniqueCardNumber": "BB1-011",
    "name": "GulusGammamon",
    "imgUrl": "/BB1-011.png",
    "cardType": "Digimon",
    "color": ["Red", "Purple"],
    "attribute": "Virus",
    "cardNumber": "BB1-011",
    "digivolveConditions": [
      { "color": "Red", "cost": 3, "level": 3 },
      { "color": "Purple", "cost": 3, "level": 3 }
    ],
    "specialDigivolve": null,
    "stage": "Champion",
    "digiType": ["Dragonkin", "Girlies"],
    "dp": 5000,
    "playCost": 5,
    "level": 4,
    "mainEffect": "[When Digivolving] By trashing 1 card from your hand, " +
      "delete 1 of your opponent's Digimon with DP less than or equal " +
      "to this Digimon's DP.",
    "inheritedEffect": "[On Deletion] Gain 1 memory.",
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": "This card is also treated as having the [Dark Dragon] trait.",
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  },
  {
    "uniqueCardNumber": "BB1-012",
    "name": "Greymon",
    "imgUrl": "/BB1-012.png",
    "cardType": "Digimon",
    "color": ["Red", "Yellow"],
    "attribute": "Vaccine",
    "cardNumber": "BB1-012",
    "digivolveConditions": [
      { "color": "Red", "cost": 3, "level": 3 },
      { "color": "Yellow", "cost": 3, "level": 3 }
    ],
    "specialDigivolve": null,
    "stage": "Champion",
    "digiType": ["Dinosaur", "Nerds", "IADA"],
    "dp": 5000,
    "playCost": 5,
    "level": 4,
    "mainEffect": "[On Play] [When Digivolving] You may play 1 Tamer card " +
      "with the [Nerds] or [IADA] trait from your hand without paying its " +
      "memory cost. This effect cannot play a Tamer card with the same " +
      "name as any of your Tamers in play. Then, if you have [Nathan James] " +
      "in play, 1 of your opponent's Digimon gets -3000 DP for the turn.",
    "inheritedEffect": "[Your Turn] This Digimon gets +2000 DP.",
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": null,
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  },
  {
    "uniqueCardNumber": "BB1-027",
    "name": "Garurumon",
    "imgUrl": "/BB1-027.png",
    "cardType": "Digimon",
    "color": ["Blue", "Black"],
    "attribute": "Vaccine",
    "cardNumber": "BB1-027",
    "digivolveConditions": [
      { "color": "Blue", "cost": 3, "level": 3 },
      { "color": "Black", "cost": 3, "level": 3 }
    ],
    "specialDigivolve": null,
    "stage": "Champion",
    "digiType": ["Beast", "IADA"],
    "dp": 5000,
    "playCost": 5,
    "level": 4,
    "mainEffect": "[On Play] [When Digivolving] You may play 1 Tamer card " +
      "with the [IADA] trait from your hand without paying the cost. This " +
      "effect cannot play a Tamer card with the same name as any of your " +
      "Tamers in play.",
    "inheritedEffect": "[Your Turn] [Once Per Turn] When a Tamer card " +
      "with the [IADA] trait is played, ＜Draw 1＞.",
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": null,
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  },
  {
    "uniqueCardNumber": "BB1-028",
    "name": "Greymon",
    "imgUrl": "/BB1-028.png",
    "cardType": "Digimon",
    "color": ["Blue"],
    "attribute": "Virus",
    "cardNumber": "BB1-028",
    "digivolveConditions": [
      { "color": "Blue", "cost": 2, "level": 2 }
    ],
    "specialDigivolve": null,
    "stage": "Champion",
    "digiType": ["Dinosaur"],
    "dp": 5000,
    "playCost": 5,
    "level": 4,
    "mainEffect": "[On Play] [When Digivolving] You may reveal the top 4 " +
      "cards of your deck. Add to your hand 1 Blue Digimon with [Greymon] " +
      "in its name and 1 [Laura Loath]. Place the rest at the bottom of " +
      "the deck in any order.",
    "inheritedEffect": "[All Turns] [Once Per Turn] If a Digimon is " +
      "deleted, you may play 1 [Laura Loath] from your hand without " +
      "paying the cost.",
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": null,
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  },
  {
    "uniqueCardNumber": "BB1-029",
    "name": "Sangloupmon",
    "imgUrl": "/BB1-029.png",
    "cardType": "Digimon",
    "color": ["Blue", "Black"],
    "attribute": "Virus",
    "cardNumber": "BB1-029",
    "digivolveConditions": [
      { "color": "Blue", "cost": 3, "level": 3 },
      { "color": "Black", "cost": 3, "level": 3 }
    ],
    "specialDigivolve": null,
    "stage": "Champion",
    "digiType": ["Dark Animal", "Girlies", "IADA"],
    "dp": 5000,
    "playCost": 5,
    "level": 4,
    "mainEffect": "＜Blocker＞\n" +
      "[On Play] [When Digivolving] ＜Draw 1＞. Then, if you have [Kazuo " +
      "Kubo] in play, until the end of your opponent's turn, 1 of your " +
      "opponent's Digimon gains \"[Start of Your Main Phase] This " +
      "Digimon attacks.\"",
    "inheritedEffect": "＜Reboot＞",
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": null,
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  },
  {
    "uniqueCardNumber": "BB1-031",
    "name": "MetalGreymon",
    "imgUrl": "/BB1-031.png",
    "cardType": "Digimon",
    "color": ["Blue"],
    "attribute": "Virus",
    "cardNumber": "BB1-031",
    "digivolveConditions": [
      { "color": "Blue", "cost": 4, "level": 3 }
    ],
    "specialDigivolve": null,
    "stage": "Ultimate",
    "digiType": ["Cyborg"],
    "dp": 7000,
    "playCost": 7,
    "level": 5,
    "mainEffect": "[On Play] [When Digivolving] You may suspend up to 2 " +
      "of your opponent's Digimon with DP as much or lower as this " +
      "Digimon's DP. Then if it's your turn, you may delete 1 of your " +
      "opponent's suspended Digimon.\n" +
      "[All Turns] While you have a [Laura Loath] in play, this card " +
      "gains +3000 DP and ＜Piercing＞.\n" +
      "[All Turns] [Once Per Turn] When any Option cards are used, gain " +
      "1 memory.",
    "inheritedEffect": "[Your Turn] [Once Per Turn] When you use a Blue " +
      "Option with [Greymon] in its text, return 1 of your opponent's " +
      "Digimon with the lowest DP to the bottom of the deck.",
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": null,
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  },
  {
    "uniqueCardNumber": "BB1-041",
    "name": "Sistermon Ciel",
    "imgUrl": "/BB1-041.png",
    "cardType": "Digimon",
    "color": ["Yellow", "Black"],
    "attribute": "Vaccine",
    "cardNumber": "BB1-041",
    "digivolveConditions": [
      { "color": "Yellow", "cost": 2, "level": 3 },
      { "color": "Black", "cost": 2, "level": 3 }
    ],
    "specialDigivolve": null,
    "stage": "Champion",
    "digiType": ["Puppet", "DC"],
    "dp": 5000,
    "playCost": 5,
    "level": 4,
    "mainEffect": "[When Digivolving] Play 1 Tamer card with the [DC] " +
      "trait from your hand without paying its memory cost. For this " +
      "turn, the Tamer played by this effect is treated as a 3000 DP " +
      "Digimon and cannot digivolve.",
    "inheritedEffect": "＜Alliance＞",
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": null,
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  },
  {
    "uniqueCardNumber": "BB1-043",
    "name": "Lynxmon",
    "imgUrl": "/BB1-043.png",
    "cardType": "Digimon",
    "color": ["Yellow", "Red"],
    "attribute": "Free",
    "cardNumber": "BB1-043",
    "digivolveConditions": [
      { "color": "Yellow", "cost": 3, "level": 3 },
      { "color": "Red", "cost": 3, "level": 3 }
    ],
    "specialDigivolve": null,
    "stage": "Champion",
    "digiType": ["Beast", "Arena"],
    "dp": 6000,
    "playCost": 6,
    "level": 4,
    "mainEffect": "＜Armor Purge＞ ＜Barrier＞\n" +
      "[End of Attack] [Once Per Turn] This Digimon may digivolve into " +
      "a Digimon card in your hand with the [Arena], [Angel], or [Holy " +
      "Beast] trait with its digivolution cost reduced by 1.",
    "inheritedEffect": null,
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": null,
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  },
  {
    "uniqueCardNumber": "BB1-054",
    "name": "Galemon",
    "imgUrl": "/BB1-054.png",
    "cardType": "Digimon",
    "color": ["Green"],
    "attribute": "Data",
    "cardNumber": "BB1-054",
    "digivolveConditions": [
      { "color": "Green", "cost": 2, "level": 3 }
    ],
    "specialDigivolve": null,
    "stage": "Champion",
    "digiType": ["Bird Dragon", "Arena", "IADA"],
    "dp": 4000,
    "playCost": 5,
    "level": 4,
    "mainEffect": "[When Attacking] Suspend 1 Digimon. Then, if attacking " +
      "an opponent's Digimon, or if you have [Orla Mallon] in play, this " +
      "Digimon may digivolve into a Digimon card with the [Arena] or " +
      "[IADA] trait with its digivolution cost reduced by 1.",
    "inheritedEffect": "[All Turns] While this Digimon is suspended, " +
      "it gets +2000 DP.",
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": null,
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  },
  {
    "uniqueCardNumber": "BB1-055",
    "name": "Weedmon",
    "imgUrl": "/BB1-055.png",
    "cardType": "Digimon",
    "color": ["Green", "Yellow"],
    "attribute": "Virus",
    "cardNumber": "BB1-055",
    "digivolveConditions": [
      { "color": "Green", "cost": 3, "level": 3 },
      { "color": "Yellow", "cost": 3, "level": 3 }
    ],
    "specialDigivolve": null,
    "stage": "Champion",
    "digiType": ["Vegetation", "Nerds", "IADA"],
    "dp": 5000,
    "playCost": 5,
    "level": 4,
    "mainEffect": "[On Play] [When Digivolving] Suspend 1 of your " +
      "opponent's Digimon. Then, if you have a Tamer with [Oryon] " +
      "in its name in play, that Digimon cannot unsuspend during " +
      "your opponent's next unsuspend phase.\n" +
      "[Your Turn] While you have a Tamer with [Oryon Sonier] in " +
      "its name in play, all your Digimon with the [IADA] or [Nerds] " +
      "trait gain ＜Piercing＞.",
    "inheritedEffect": "[Your Turn] [Once Per Turn] When an opponent's " +
      "Digimon becomes suspended, gain 1 memory.",
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": null,
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  },
  {
    "uniqueCardNumber": "BB1-057",
    "name": "Peckmon",
    "imgUrl": "/BB1-057.png",
    "cardType": "Digimon",
    "color": ["Green", "Purple"],
    "attribute": "Vaccine",
    "cardNumber": "BB1-057",
    "digivolveConditions": [
      { "color": "Green", "cost": 3, "level": 3 },
      { "color": "Purple", "cost": 3, "level": 3 }
    ],
    "specialDigivolve": null,
    "stage": "Champion",
    "digiType": ["Avian", "DC"],
    "dp": 4000,
    "playCost": 4,
    "level": 4,
    "mainEffect": "[On Play] [When Digivolving] Suspend 1 of your " +
      "opponent's Tamers. Then, if your opponent has no unsuspended " +
      "Tamers, trash the top 2 cards of their deck.",
    "inheritedEffect": "[Your Turn] [Once Per Turn] When an opponent's " +
      "Tamer suspends, gain 1 memory.",
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": null,
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  },
  {
    "uniqueCardNumber": "BB1-065",
    "name": "Sangloupmon",
    "imgUrl": "/BB1-065.png",
    "cardType": "Digimon",
    "color": ["Black", "Purple"],
    "attribute": "Virus",
    "cardNumber": "BB1-065",
    "digivolveConditions": [
      { "color": "Black", "cost": 3, "level": 3 },
      { "color": "Purple", "cost": 3, "level": 3 }
    ],
    "specialDigivolve": null,
    "stage": "Champion",
    "digiType": ["Dark Animal", "CT"],
    "dp": 5000,
    "playCost": 5,
    "level": 4,
    "mainEffect": "[On Play] [When Digivolving] Trash 1 card from " +
      "your hand. Then, you may place 1 card with the [CT] trait from " +
      "your trash under this Digimon as its bottom digivolution " +
      "card. If this Digimon has 3 or more digivolution cards, ＜Draw 1＞.",
    "inheritedEffect": "[All Turns] While this Digimon has 3 or more " +
      "digivolution cards, it has ＜Reboot＞.",
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": null,
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  },
  {
    "uniqueCardNumber": "BB1-066",
    "name": "Minotarumon",
    "imgUrl": "/BB1-066.png",
    "cardType": "Digimon",
    "color": ["Black", "Purple"],
    "attribute": "Virus",
    "cardNumber": "BB1-066",
    "digivolveConditions": [
      { "color": "Black", "cost": 3, "level": 3 },
      { "color": "Purple", "cost": 3, "level": 3 }
    ],
    "specialDigivolve": null,
    "stage": "Champion",
    "digiType": ["Beastkin", "ME"],
    "dp": 5000,
    "playCost": 5,
    "level": 4,
    "mainEffect": "＜Collision＞\n" +
      "[On Play] [When Digivolving] By trashing 1 card with the [ME] " +
      "trait from your hand, ＜De-Digivolve 1＞ 1 of your opponent's " +
      "Digimon.",
    "inheritedEffect": "[When Attacking] [Once Per Turn] Reveal the " +
      "top 3 cards of your deck. For each card with the [ME] trait, " +
      "this Digimon gets +1000 DP until the end of your opponent's turn. " +
      "Trash the remaining cards.",
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": null,
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  },
  {
    "uniqueCardNumber": "BB1-067",
    "name": "Deputymon",
    "imgUrl": "/BB1-067.png",
    "cardType": "Digimon",
    "color": ["Black"],
    "attribute": "Vaccine",
    "cardNumber": "BB1-067",
    "digivolveConditions": [
      { "color": "Black", "cost": 2, "level": 3 }
    ],
    "specialDigivolve": null,
    "stage": "Champion",
    "digiType": ["Mutant"],
    "dp": 6000,
    "playCost": 6,
    "level": 4,
    "mainEffect": "[On Play] [When Digivolving] By placing 1 card " +
      "with the [Three Musketeers] trait from your hand or trash under " +
      "1 of your Digimon as its bottom digivolution card, this Digimon's " +
      "DP cannot be reduced by your opponent's effects until the end of " +
      "their turn.",
    "inheritedEffect": "[Your Turn] [Once Per Turn] When cards are " +
      "placed in this Digimon's digivolution cards, ＜Draw 1＞.",
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": "This card is also treated as having the [Three Musketeers] trait.",
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  },
  {
    "uniqueCardNumber": "BB1-076",
    "name": "Seasamon",
    "imgUrl": "/BB1-076.png",
    "cardType": "Digimon",
    "color": ["Purple", "Green"],
    "attribute": "Vaccine",
    "cardNumber": "BB1-076",
    "digivolveConditions": [
      { "color": "Green", "cost": 2, "level": 3 },
      { "color": "Purple", "cost": 2, "level": 3 }
    ],
    "specialDigivolve": null,
    "stage": "Champion",
    "digiType": ["Holy Beast", "Re;Ware", "Girlies"],
    "dp": 6000,
    "playCost": 5,
    "level": 4,
    "mainEffect": "[When Moving] [When Digivolving] If a Digimon " +
      "with the [Re;Ware] trait and/or [Girlies] trait is in this " +
      "Digimon's Digivolution cards, then you may play 1 tamer with " +
      "the [Re;Ware] trait and/or [Girlies] trait from your hand " +
      "without paying the cost. Then, you may place 1 card with the " +
      "[Re;Ware] trait and/or [Girlies] trait from your hand or trash " +
      "as this Digimon's bottom Digivolution card.",
    "inheritedEffect": "[Main] [Once Per Turn] By suspending 1 " +
      "Digimon or Tamer with the [Re;Ware] trait and/or [Girlies] " +
      "trait, Gain 1 Memory.",
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": null,
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  },
  {
    "uniqueCardNumber": "BB1-077",
    "name": "Bakemon",
    "imgUrl": "/BB1-077.png",
    "cardType": "Digimon",
    "color": ["Purple"],
    "attribute": "Virus",
    "cardNumber": "BB1-077",
    "digivolveConditions": [
      { "color": "Purple", "cost": 2, "level": 3 }
    ],
    "specialDigivolve": null,
    "stage": "Champion",
    "digiType": ["Ghost", "CT"],
    "dp": 4000,
    "playCost": 4,
    "level": 4,
    "mainEffect": "[Main] [Once Per Turn] You may play or use 1 card " +
      "with the [CT] trait from your hand with its play cost or use " +
      "cost reduced by 2.",
    "inheritedEffect": "[Your Turn] [Once Per Turn] When you use an " +
      "Option card with the [CT] trait, ＜Draw 1＞.",
    "aceEffect": null,
    "burstDigivolve": null,
    "digiXros": null,
    "dnaDigivolve": null,
    "securityEffect": null,
    "rule": null,
    "linkDP": null,
    "linkEffect": null,
    "linkRequirement": null,
    "assemblyEffect": null,
    "restrictions": { 
      "chinese": "Unrestricted", 
      "english": "Unrestricted", 
      "japanese": "Unrestricted", 
      "korean": "Unrestricted" 
    },
    "illustrator": "Custom BB1 Art",
    "_class": "com.github.wekaito.backend.models.Card"
  }
]);