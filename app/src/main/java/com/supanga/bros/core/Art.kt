package com.supanga.bros.core

/**
 * All pixel art of the game as ASCII. Every sprite is original artwork made for Supa NGA Bros.
 * '.' is transparent; letters are resolved through the palettes in [Sprites].
 */
object Art {

    // ---------------------------------------------------------------- hero (small, 16x16, facing right)

    private val smallHead = listOf(
        "......KKK.......",
        ".....KWWWK......",
        "....KKKWKKKK....",
        "...KCCCCCCCCK...",
        "..KCCCCCCCCCCK..",
        "..KWWWWWWWWWWK..",
        "..KHHSSSSSKSSK..",
        "..KHSSSSSSKSSSK.",
        "...KSSSSSSSSSK..",
        "....KKSSSSSKK...",
    )

    val smallStand = smallHead + listOf(
        "...KCCCCCCCK....",
        "..KSCOOOOOCSK...",
        "...KOYOOOYOK....",
        "...KOOOOOOOK....",
        "..KHHHK.KHHHK...",
        "..KKKKK.KKKKK...",
    )

    val smallWalk1 = smallHead + listOf(
        "...KCCCCCCCK....",
        "..KSCOOOOOCCSK..",
        "...KOOOOOOOOK...",
        "..KOOOKKKOOOK...",
        ".KHHHK...KHHHK..",
        ".KKKKK...KKKKK..",
    )

    val smallWalk2 = smallHead + listOf(
        "...KCCCCCCCK....",
        "...KSCOOOCSK....",
        "....KOOOOOK.....",
        "....KOOOOOK.....",
        "...KHHHHHHK.....",
        "...KKKKKKKK.....",
    )

    val smallWalk3 = smallHead + listOf(
        "...KCCCCCCCK....",
        "..KSKCOOOCKSK...",
        "...KOOOOOOOK....",
        "...KOOOKOOOK....",
        "..KHHHKKHHHK....",
        "..KKKKK.KKKK....",
    )

    val smallJump = smallHead + listOf(
        "...KCCCCCCCKKK..",
        "..KCCOOOOOCCSSK.",
        "..KSKOOOOOOKKK..",
        "...KOOOOOOOOK...",
        "..KHHHK..KOOOK..",
        "..KKKK...KHHHK..",
    )

    val smallSkid = smallHead + listOf(
        "..KCCCCCCCK.....",
        ".KSCCOOOOCCK....",
        "..KKOOOOOOK.....",
        "..KOOOOKOOOK....",
        ".KHHHK.KHHHK....",
        ".KKKKK.KKKKK....",
    )

    val smallDead = listOf(
        "......KKKK......",
        ".KK..KWWWWK..KK.",
        "KSSK.KKKKKK.KSSK",
        "KSSKKCCCCCCKKSSK",
        ".KCKCCCCCCCCKCK.",
        ".KCKWWWWWWWWKCK.",
        "..KKSKSSSSKSKK..",
        "...KSKSSSSKSK...",
        "...KSSSKKSSSK...",
        "....KKSKKSKK....",
        "...KCCOOOOCCK...",
        "...KCOYOOYOCK...",
        "...KOOOOOOOOK...",
        "...KOOOKKOOOK...",
        "..KHHHK..KHHHK..",
        "..KKKKK..KKKKK..",
    )

    // ---------------------------------------------------------------- hero (big, 16x32, facing right)

    private val bigHead = listOf(
        "......KKK.......",
        ".....KWWWK......",
        ".....KWWWK......",
        "....KKKWKKKK....",
        "...KCCCCCCCCK...",
        "..KCCCCCCCCCCK..",
        "..KCCCCCCCCCCK..",
        "..KWWWWWWWWWWK..",
        "..KHHSSSSSKKSK..",
        "..KHHSSSSSKKSSK.",
        "..KHSSSSSSKKSSSK",
        "..KHSSSSSSSSSSK.",
        "...KSSSSSSSKKK..",
        "....KKSSSSSSK...",
    )

    private val bigTorso = listOf(
        "...KCCCCCCCCK...",
        "..KCCCCOOCCCCK..",
        ".KCCCCOOOOCCCCK.",
        ".KCCCKOYYOKCCCK.",
    )

    val bigStand = bigHead + bigTorso + listOf(
        ".KCCKOOOOOOKCCK.",
        ".KSSKOOOOOOKSSK.",
        ".KSSKOOOOOOKSSK.",
        "..KKOOOOOOOOKK..",
        "...KOOOOOOOOK...",
        "...KOOOOOOOOK...",
        "...KOOOKKOOOK...",
        "...KOOOKKOOOK...",
        "...KOOOKKOOOK...",
        "...KOOOKKOOOK...",
        "..KHHHHKKHHHHK..",
        "..KHHHHKKHHHHK..",
        "..KHHHHKKHHHHK..",
        "..KKKKKKKKKKKK..",
    )

    val bigWalk1 = bigHead + bigTorso + listOf(
        "..KCKOOOOOOKCCK.",
        "..KSKOOOOOOKSSK.",
        "..KKKOOOOOOKSSK.",
        "...KOOOOOOOOK...",
        "..KOOOOOOOOOOK..",
        "..KOOOOKKOOOOK..",
        ".KOOOOK..KOOOOK.",
        ".KOOOK....KOOOK.",
        "KOOOK......KOOOK",
        "KOOOK......KOOOK",
        "KHHHK......KHHHK",
        "KHHHHK....KHHHHK",
        "KHHHHK....KHHHHK",
        "KKKKKK....KKKKKK",
    )

    val bigWalk2 = bigHead + bigTorso + listOf(
        ".KCCKOOOOOOKCCK.",
        "..KSKOOOOOOKSK..",
        "..KSKOOOOOOKSK..",
        "...KOOOOOOOOK...",
        "...KOOOOOOOOK...",
        "....KOOOOOOK....",
        "....KOOOOOOK....",
        "....KOOOOOOK....",
        "....KOOOKOOK....",
        "....KOOOKOOK....",
        "...KHHHHHHHK....",
        "...KHHHHHHHHK...",
        "...KHHHHHHHHK...",
        "...KKKKKKKKKK...",
    )

    val bigWalk3 = bigHead + bigTorso + listOf(
        ".KCCKOOOOOOKCCK.",
        ".KSSKOOOOOOKSSK.",
        "..KKKOOOOOOKKK..",
        "...KOOOOOOOOK...",
        "...KOOOOOOOOK...",
        "...KOOOOKOOOOK..",
        "..KOOOOK.KOOOK..",
        "..KOOOK...KOOOK.",
        "..KOOOK...KOOOK.",
        ".KOOOK....KOOOK.",
        ".KHHHK....KHHHK.",
        ".KHHHHK...KHHHHK",
        ".KHHHHK...KHHHHK",
        ".KKKKKK...KKKKKK",
    )

    val bigJump = bigHead + listOf(
        "...KCCCCCCCCKKSK",
        "..KCCCCOOCCCCKSK",
        ".KCCCCOOOOCCCCK.",
        ".KCCCKOYYOKCCK..",
        ".KSSKOOOOOOKK...",
        ".KSSKOOOOOOK....",
        "..KKOOOOOOOK....",
        "...KOOOOOOOOK...",
        "..KOOOOOOOOOOK..",
        ".KOOOOOKKOOOOOK.",
        ".KOOOOK..KOOOOK.",
        "KOOOOK....KOOOK.",
        "KHHHK.....KOOOK.",
        "KHHHHK....KOOOK.",
        "KKKKKK...KHHHHK.",
        "........KHHHHHK.",
        "........KKKKKKK.",
        "................",
    )

    val bigSkid = bigHead + listOf(
        "..KCCCCCCCCK....",
        ".KCCCCOOCCCCK...",
        "KCCCCOOOOCCCCK..",
        "KSSCKOYYOKCCCK..",
        "KSSKOOOOOOKCCK..",
        ".KKKOOOOOOKSSK..",
        "...KOOOOOOKSSK..",
        "...KOOOOOOOKK...",
        "..KOOOOOOOOOK...",
        "..KOOOOKKOOOOK..",
        ".KOOOOK..KOOOK..",
        ".KOOOK...KOOOK..",
        "KOOOK....KOOOK..",
        "KOOOK....KOOOK..",
        "KHHHHK...KHHHHK.",
        "KHHHHK...KHHHHK.",
        "KHHHHK...KHHHHK.",
        "KKKKKK...KKKKKK.",
    )

    val bigDuck = List(12) { "................" } + bigHead + listOf(
        "..KCCOOOOOOCCK..",
        ".KSSKOOOOOOKSSK.",
        ".KSSKOOOOOOKSSK.",
        "..KHHHHKKHHHHK..",
        ".KHHHHHKKHHHHHK.",
        ".KKKKKKKKKKKKKK.",
    )

    // ---------------------------------------------------------------- enemies

    val walker = listOf(
        "................",
        "......KKKK......",
        "....KKaaaaKK....",
        "...KaaaaaaaaK...",
        "..KAaaaaaaaaAK..",
        "..KAAKKAAKKAAK..",
        ".KAAAAKKKKAAAAK.",
        ".KAAWWKAAKWWAAK.",
        ".KAAWKKAAKKWAAK.",
        ".KAAAAAAAAAAAAK.",
        ".KAAKTKKKKTKAAK.",
        "..KAAKKKKKKAAK..",
        "...KKTTTTTTKK...",
        "...KFFKKKKFFK...",
        "..KFFFFK.KFFFK..",
        "..KKKKK...KKKK..",
    )

    val walkerFlat = List(10) { "................" } + listOf(
        "....KKKKKKKK....",
        "..KKaaaaaaaaKK..",
        ".KAAKKAAAAKKAAK.",
        ".KAAWKAAAAKWAAK.",
        "..KKKKKKKKKKKK..",
        ".KFFFK....KFFFK.",
    )

    private val turtleShellTop = listOf(
        "......KKKKK.....",
        "....KKGLGLGKK...",
        "...KGLGGLGGLGK..",
        "..KGGLGGLGGLGGK.",
    )

    val turtle1 = List(4) { "................" } + turtleShellTop + listOf(
        ".KKGLGGLGGLGGLGK",
        "KSSKKKKKKKKKKKKK",
        "KWKSKYYYYYYYYYYK",
        "KSSSSKKKKKKKKKK.",
        ".KKSSK...KSSK...",
        "...KSSK..KSSK...",
        "...KSSK..KSSK...",
        "...KKKK..KKKK...",
    )

    val turtle2 = List(4) { "................" } + turtleShellTop + listOf(
        ".KKGLGGLGGLGGLGK",
        "KSSKKKKKKKKKKKKK",
        "KWKSKYYYYYYYYYYK",
        "KSSSSKKKKKKKKKK.",
        ".KKSSK..KSSK....",
        "..KSSK...KSSK...",
        "..KSSK...KSSK...",
        "..KKKK...KKKK...",
    )

    val shell = List(8) { "................" } + turtleShellTop + listOf(
        ".KGGLGGLGGLGGLGK",
        ".KKKKKKKKKKKKKKK",
        ".KYYYYYYYYYYYYYK",
        "..KKKKKKKKKKKKK.",
    )

    val shellWaking = List(8) { "................" } + turtleShellTop + listOf(
        ".KGGLGGLGGLGGLGK",
        "KSSKKKKKKKKKKKKK",
        "KWKSYYYYYYYYYYYK",
        ".KKKKKSSKKKSSKK.",
    )

    val wing = listOf(
        "..KK....",
        ".KWWK...",
        "KWWWWK..",
        "KWWWWWK.",
        ".KWWWWWK",
        "..KKWWK.",
        "....KK..",
        "........",
    )

    private val biterStem = listOf(
        "......KGGK......",
        "..KK..KGGK..KK..",
        ".KLLK.KGGK.KLLK.",
        "KLLLLKKGGKKLLLLK",
        "KLLLLLKGGKLLLLLK",
        ".KLLLLKGGKLLLLK.",
        "..KKKLKGGKLKKK..",
        "......KGGK......",
        "......KGGK......",
        "......KGGK......",
        "......KGGK......",
        "......KGGK......",
    )

    val biterOpen = listOf(
        "..KK........KK..",
        ".KRRK......KRRK.",
        ".KRWRK....KRWRK.",
        "KRRRRK....KRRRRK",
        "KRRRRTK..KTRRRRK",
        "KRWRRRTKKTRRRWRK",
        "KRRRRRRKKRRRRRRK",
        ".KRRRRRRRRRRRRK.",
        ".KRRWRRRRRRWRRK.",
        "..KRRRRRRRRRRK..",
        "...KKRRRRRRKK...",
        ".....KKKKKK.....",
    ) + biterStem

    val biterClosed = listOf(
        "................",
        "......KKKK......",
        "....KKRRRRKK....",
        "...KRRWRRRRRK...",
        "..KRRRRRRRWRRK..",
        "..KRWRRTTRRRRK..",
        ".KRRRRTKKTRRWRK.",
        ".KRRRRRKKRRRRRK.",
        ".KRRWRRKKRRRRRK.",
        "..KRRRRKKRRRWK..",
        "...KKRRKKRRKK...",
        ".....KKKKKK.....",
    ) + biterStem

    // ---------------------------------------------------------------- items

    val mushroom = listOf(
        "......KKKK......",
        "....KKMMMMKK....",
        "...KMMWWWMMMK...",
        "..KMMWWWWWMMMK..",
        ".KMMMWWWWWMMMMK.",
        ".KWWMMWWWMMMWWK.",
        "KWWWWMMMMMMWWWWK",
        "KWWWWMMMMMMWWWWK",
        "KMWWMMMMMMMMWWMK",
        "KMMMMKKKKKKMMMMK",
        ".KKKKTTTTTTKKKK.",
        "...KTTKTTKTTK...",
        "...KTTKTTKTTK...",
        "...KTTTTTTTTK...",
        "....KTTTTTTK....",
        ".....KKKKKK.....",
    )

    val flower = listOf(
        "................",
        "....KKKKKKKK....",
        "..KKPPPPPPPPKK..",
        ".KPPYYYYYYYYPPK.",
        "KPPYYWWWWWWYYPPK",
        ".KPPYYYYYYYYPPK.",
        "..KKPPPPPPPPKK..",
        "....KKKKKKKK....",
        ".......GG.......",
        ".KKK...GG...KKK.",
        "KLLLK..GG..KLLLK",
        "KLLLLK.GG.KLLLLK",
        ".KLLLLKGGKLLLLK.",
        "..KKLLKGGKLLKK..",
        "....KKKGGKKK....",
        "......KGGK......",
    )

    val star = listOf(
        ".......KK.......",
        "......KSSK......",
        "......KSSK......",
        ".....KSSSSK.....",
        "KKKKKKSSSSKKKKKK",
        "KSSSSSSSSSSSSSSK",
        ".KSSSSKSSKSSSSK.",
        "..KSSSKSSKSSSK..",
        "...KSSSSSSSSK...",
        "...KSSSSSSSSK...",
        "..KSSSSSSSSSSK..",
        "..KSSSSKKSSSSK..",
        ".KSSSKK..KKSSSK.",
        ".KSSK......KSSK.",
        "KSKK........KKSK",
        "KK............KK",
    )

    val coin = listOf(
        "................",
        "......KKKK......",
        ".....KYYYYK.....",
        "....KYWWYYDK....",
        "....KYWYYYDK....",
        "...KYWYYYYYDK...",
        "...KYWYYYYYDK...",
        "...KYWYYYYYDK...",
        "...KYWYYYYYDK...",
        "...KYWYYYYYDK...",
        "...KYYYYYYYDK...",
        "....KYYYYYDK....",
        "....KYYYYDDK....",
        ".....KDDDDK.....",
        "......KKKK......",
        "................",
    )

    val hudCoin = listOf(
        "...KK...",
        "..KYYK..",
        "..KWYK..",
        "..KWYK..",
        "..KWYK..",
        "..KYDK..",
        "...KK...",
        "........",
    )

    val fireball = listOf(
        "...RR...",
        "..RYYR..",
        ".RYWWYR.",
        "RYWWWYYR",
        "RYYWWYR.",
        ".RYYYR..",
        "..RRR...",
        "........",
    )

    val debris = listOf(
        "..KKKK..",
        ".KAAAAK.",
        "KAAKAAAK",
        "KAAKAAAK",
        "KKKKKKKK",
        "KAAAKAAK",
        ".KAAKAK.",
        "..KKKK..",
    )

    // ---------------------------------------------------------------- tiles (16x16)

    val ground = listOf(
        "LLLLLLLLLLLLLLLD",
        "LAAAAAAAAAAAAAAD",
        "LAALLAAAAAAAAAAD",
        "LALAAAAAAADAAAAD",
        "LAAAAAAAAAAAAAAD",
        "LAAAAAAAAAAAAAAD",
        "LAAAAAAADAAAAAAD",
        "LAAAAAAAAAAAALAD",
        "DDDDDDDDLLLLLLLD",
        "LLLLLLLDLAAAAAAD",
        "LAAADAADLAAAAAAD",
        "LAAAAAADLAAADAAD",
        "LAAAAAADLAAAAAAD",
        "LAALAAADLAAAAAAD",
        "LAAAAAADLAAAAAAD",
        "DDDDDDDDDDDDDDDD",
    )

    val brick = listOf(
        "LLLLLLLLLLLLLLLL",
        "LAAAAAAKLAAAAAAA",
        "AAAAAAAKAAAAAAAA",
        "AAAAAAAKAAAAAAAA",
        "KKKKKKKKKKKKKKKK",
        "AAAKLAAAAAAKLAAA",
        "AAAKAAAAAAAKAAAA",
        "AAAKAAAAAAAKAAAA",
        "KKKKKKKKKKKKKKKK",
        "LAAAAAAKLAAAAAAA",
        "AAAAAAAKAAAAAAAA",
        "AAAAAAAKAAAAAAAA",
        "KKKKKKKKKKKKKKKK",
        "AAAKLAAAAAAKLAAA",
        "AAAKAAAAAAAKAAAA",
        "KKKKKKKKKKKKKKKK",
    )

    val question = listOf(
        ".KKKKKKKKKKKKKK.",
        "KQQQQQQQQQQQQQQK",
        "KQDQQQQQQQQQQDQK",
        "KQQQQWWWWWQQQQQK",
        "KQQQWWDDDWWQQQQK",
        "KQQQWWDQQWWDQQQK",
        "KQQQQDDQQWWDQQQK",
        "KQQQQQQQWWDDQQQK",
        "KQQQQQQWWDDQQQQK",
        "KQQQQQQWWDQQQQQK",
        "KQQQQQQQDDQQQQQK",
        "KQQQQQQWWQQQQQQK",
        "KQQQQQQWWDQQQQQK",
        "KQDQQQQQDDQQQDQK",
        "KQQQQQQQQQQQQQQK",
        ".KKKKKKKKKKKKKK.",
    )

    val used = listOf(
        ".KKKKKKKKKKKKKK.",
        "KAAAAAAAAAAAAAAK",
        "KAKAAAAAAAAAAKAK",
        "KAAAAAAAAAAAAAAK",
        "KAAAAAAAAAAAAAAK",
        "KAAAAAAAAAAAAAAK",
        "KAAAAAAAAAAAAAAK",
        "KAAAAAAAAAAAAAAK",
        "KAAAAAAAAAAAAAAK",
        "KAAAAAAAAAAAAAAK",
        "KAAAAAAAAAAAAAAK",
        "KAAAAAAAAAAAAAAK",
        "KAAAAAAAAAAAAAAK",
        "KAKAAAAAAAAAAKAK",
        "KAAAAAAAAAAAAAAK",
        ".KKKKKKKKKKKKKK.",
    )

    val hard = listOf(
        "LLLLLLLLLLLLLLLD",
        "LLLLLLLLLLLLLLDD",
        "LLAAAAAAAAAAAADD",
        "LLAAAAAAAAAAAADD",
        "LLAAAAAAAAAAAADD",
        "LLAAAAAAAAAAAADD",
        "LLAAAAAAAAAAAADD",
        "LLAAAAAAAAAAAADD",
        "LLAAAAAAAAAAAADD",
        "LLAAAAAAAAAAAADD",
        "LLAAAAAAAAAAAADD",
        "LLAAAAAAAAAAAADD",
        "LLAAAAAAAAAAAADD",
        "LLAAAAAAAAAAAADD",
        "LDDDDDDDDDDDDDDD",
        "DDDDDDDDDDDDDDDD",
    )

    /** 32 wide: lip of a vertical pipe, split into two tiles at runtime. */
    const val pipeLipRow = "KDLLGLLLGGGGGGGGGGGGGGGDGDDGDDDK"
    const val pipeBodyRow = "..KDLLGLLLGGGGGGGGGGGGGDGDDGDK.."

    val treeLeft = listOf(
        ".....KKKKKKKKKKK",
        "...KKLLLLLLLLLLL",
        "..KLLLLGLLLLLLLL",
        ".KLLLLGGGLLLLGLL",
        ".KLLLLLGLLLLLLLL",
        "KLLLLLLLLLLGLLLL",
        "KLLGLLLLLLGGGLLL",
        "KLGGGLLLLLLGLLLL",
        "KLLGLLLLLLLLLLLL",
        "KGLLLLLLLGLLLLLL",
        ".KGGLLLLGGGLLLLL",
        ".KGGGGLLLGLLLLGL",
        "..KGGGGGGGGGGGGG",
        "...KKGGGGGGGGGGG",
        ".....KKKKKKKKKKK",
        "................",
    )

    val treeMid = listOf(
        "KKKKKKKKKKKKKKKK",
        "LLLLLLLLLLLLLLLL",
        "LLLLLLLLLGLLLLLL",
        "LLGLLLLLGGGLLLLL",
        "LGGGLLLLLGLLLLLL",
        "LLGLLLLLLLLLLGLL",
        "LLLLLLLLLLLLGGGL",
        "LLLLLGLLLLLLLGLL",
        "LLLLGGGLLLLLLLLL",
        "LLLLLGLLLLLLLLLL",
        "LLLLLLLLLLGLLLLL",
        "LGLLLLLLLGGGLLLL",
        "GGGGGGGGGGGGGGGG",
        "GGGGGGGGGGGGGGGG",
        "KKKKKKKKKKKKKKKK",
        "................",
    )

    val trunk = listOf(
        "ADAAAAADAAAAADAA",
        "ADAAAAADAAAAADAA",
        "AAAADAADAAADAAAA",
        "AAAADAAAAAADAAAD",
        "DAAADAAAADAAAAAD",
        "DAAAAAAAADAAADAA",
        "AADAAAADAAAAADAA",
        "AADAAAADAAADAAAA",
        "AAAAADAAAAADAAAD",
        "ADAAADAAAAAAAAAD",
        "ADAAAAAADAAADAAA",
        "AAAADAAADAAADAAA",
        "AAAADAAAAAADAAAA",
        "DAAAAAADAAADAADA",
        "DAADAAADAAAAAADA",
        "AAADAAAAADAAAAAA",
    )

    val bridge = listOf(
        "KKKKKKKKKKKKKKKK",
        "LLLLLLLLLLLLLLLL",
        "AAKAAAKAAAKAAAKA",
        "AKLKAKLKAKLKAKLK",
        "KAAAKAAAKAAAKAAA",
        "AAAAAAAAAAAAAAAA",
        "DDDDDDDDDDDDDDDD",
        "KKKKKKKKKKKKKKKK",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    )

    val lavaTop = listOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        "....OO......OO..",
        "..OORROO..OORRO.",
        "OORRRRRROORRRRRR",
        "RRRRRRRRRRRRRRRR",
        "RRRROORRRRRRRRRR",
        "RRRRRRRRRRROORRR",
        "RRRRRRRRRRRRRRRR",
        "RROORRRRRRRRRRRR",
        "RRRRRRRRROORRRRR",
        "RRRRRRRRRRRRRRRR",
        "RRRRRROORRRRRRRR",
    )

    val lava = listOf(
        "RRRRRRRRRRRRRRRR",
        "RRRROORRRRRRRRRR",
        "RRRRRRRRRRROORRR",
        "RRRRRRRRRRRRRRRR",
        "RROORRRRRRRRRRRR",
        "RRRRRRRRROORRRRR",
        "RRRRRRRRRRRRRRRR",
        "RRRRRROORRRRRRRR",
        "RRRRRRRRRRRRRRRR",
        "RRRROORRRRRRRRRR",
        "RRRRRRRRRRROORRR",
        "RRRRRRRRRRRRRRRR",
        "RROORRRRRRRRRRRR",
        "RRRRRRRRROORRRRR",
        "RRRRRRRRRRRRRRRR",
        "RRRRRROORRRRRRRR",
    )

    val axe = listOf(
        "......KKKK......",
        "....KKWWWWKK....",
        "...KWWLLLLWWK...",
        "..KWLLLLLLLLWK..",
        "..KWLLLLLLLLWK..",
        "..KWLLLKKLLLWK..",
        "..KWLLKHHKLLWK..",
        "...KWWKHHKWWK...",
        "....KKKHHKKK....",
        "......KHHK......",
        "......KHHK......",
        "......KHHK......",
        "......KHHK......",
        "......KHHK......",
        "......KHHK......",
        "......KKKK......",
    )

    val chain = listOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "............KK..",
        "...........KGGK.",
        "..........KGKKG.",
        ".........KGK..KK",
        "........KGK.....",
        ".......KGK......",
    )

    val liftSegment = listOf(
        "KKKKKKKKKKKKKKKK",
        "KLLLLLLLLLLLLLLK",
        "KLAKAAAAAAAAKAAK",
        "KLAAAAAAAAAAAAAK",
        "KLAAAAAAAAAAAAAK",
        "KLAKAAAAAAAAKAAK",
        "KAAAAAAAAAAAAAAK",
        "KKKKKKKKKKKKKKKK",
    )

    val flag = listOf(
        "KKKKKKKKKKKKKKKK",
        ".KWWWWWWWWWWWWWW",
        "..KWWWWWRRRWWWWW",
        "...KWWWRRRRRWWWW",
        "....KWWRRWRRWWWW",
        ".....KWRRRRRWWWW",
        "......KWRRRWWWWW",
        ".......KWWWWWWWW",
        "........KWWWWWWW",
        ".........KWWWWWW",
        "..........KWWWWW",
        "...........KWWWW",
        "............KWWW",
        ".............KWW",
        "..............KW",
        "...............K",
    )

    val poleTop = listOf(
        "..KKKK..",
        ".KGGGGK.",
        "KGLLGGGK",
        "KGLGGGGK",
        "KGGGGGGK",
        "KGGGGGGK",
        ".KGGGGK.",
        "..KKKK..",
    )

    // ---------------------------------------------------------------- castle level

    val boss = listOf(
        "..KK....KK......................",
        ".KWK...KWK......................",
        ".KWWK.KWWK......................",
        "..KWWKWWK.......................",
        "..KKGGGGKK..........KK..KK......",
        ".KGGGGGGGGK........KWWKKWWK.....",
        "KGGGGGGGGGGK......KOWWOOWWOK....",
        "KGGKKKGGGGGGK....KOOOOOOOOOOK...",
        "KGKWRKGGGGGGK...KOOWWOOOOWWOOK..",
        "KGKWRKGGGGGGGK.KOOWWKOOOWWKOOOK.",
        "KGGKKGGGGGGGGGKOOOOOOOOOOOOOOOK.",
        "KGGGGGGGGGGGGGKOOWWOOOOWWOOOOOOK",
        "KKGGGGGGGGGGGKOOOWWKOOOWWKOOOOOK",
        "KWKKKKKKGGGGGKOOOOOOOOOOOOOOOOOK",
        "KWWWWWWKGGGGKOOOWWOOOOWWOOOOOOK.",
        ".KKKKKKGGGGGKOOOOWWKOOOWWKOOOOK.",
        "....KGGGGGGYYKOOOOOOOOOOOOOOOK..",
        "...KGGKGGGYYYYKOOOOWWOOOOOOOK...",
        "..KGGKWKGGYYYYYKOOOOOOOOOOOK....",
        "..KGKWKGGYYYYYYYKOOOOOOOOOK.....",
        "...KKKGGGYYYYYYYYKKOOOOOOK......",
        ".....KGGGYYYYYYYYYGKKKKKK.......",
        ".....KGGGGYYYYYYYYGGGGGGKK......",
        "......KGGGGYYYYYYGGGGGGGGGKK....",
        "......KGGGGGGGGGGGGKKKGGGGGGKK..",
        ".......KGGGGGGGGGGK...KKKKGGGWK.",
        "......KGGGGKKKKGGGGK.....KKKKK..",
        ".....KGGGGK....KGGGGK...........",
        "....KGGGGK......KGGGGK..........",
        "...KWGWGWK.....KWGWGWK..........",
        "...KKKKKKK.....KKKKKKK..........",
        "................................",
    )

    /** Rows 12..16 of [boss] replaced when the mouth is open. */
    val bossMouthOpen = listOf(
        "KKGGGGGGGGGGGKOOOWWKOOOWWKOOOOOK",
        "KWKWKWKKGGGGGKOOOOOOOOOOOOOOOOOK",
        "KrrrrrrKGGGGKOOOWWOOOOWWOOOOOOK.",
        "KWKWKWKGGGGGKOOOOWWKOOOWWKOOOOK.",
        ".KKKKKKGGGGYYKOOOOOOOOOOOOOOOK..",
    )

    val bossFire = listOf(
        "........RRRRRR....RR....",
        "....RRRROOOOOORRRROORR..",
        "..RROOOOOYYYYOOOOOOOORRR",
        "RROOYYYYYWWWWYYYYOOOOOOR",
        "RROOYYYYYWWWWYYYYOOOOOOR",
        "..RROOOOOYYYYOOOOOOOORRR",
        "....RRRROOOOOORRRROORR..",
        "........RRRRRR....RR....",
    )

    val podoboo = listOf(
        "......RRRR......",
        "....RROOOORR....",
        "...ROOYYYYOOR...",
        "..ROYYYWWYYYOR..",
        "..ROYYWWWWYYOR..",
        ".ROYYWWWWWWYYOR.",
        ".ROYYWWWWWWYYOR.",
        ".ROYYYWWWWYYYOR.",
        "..ROYYYYYYYYOR..",
        "..RROOYYYYOORR..",
        "...RROOOOOORR...",
        "....RR.OO.RR....",
        "......R..R......",
        "................",
        "................",
        "................",
    )

    val retainer = listOf(
        "......KKKK......",
        "....KKPPWWKK....",
        "...KPPPWWWPPK...",
        "..KPWWPPPPPWWK..",
        ".KPWWWPPPPPWWWK.",
        ".KPPWPPPWWPPWPK.",
        "KPPPPPPWWWWPPPPK",
        "KWWPPPPPWWPPPWWK",
        "KWWWKKKKKKKKWWWK",
        ".KKKSSSSSSSSKKK.",
        "...KSSKSSKSSK...",
        "...KSSKSSKSSK...",
        "...KSSSSSSSSK...",
        "....KSSKKSSK....",
        ".....KKSSKK.....",
        "....KBBBBBBK....",
        "...KSBBYYBBSK...",
        "...KSBBBBBBSK...",
        "....KBBBBBBK....",
        "....KWWWWWWK....",
        "....KWWKKWWK....",
        "...KHHK..KHHK...",
        "...KHHK..KHHK...",
        "...KKKK..KKKK...",
    )
}
