package trandemo.server;

/* This file is part of VoltDB.
 * Copyright (C) 2008-2022 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

public class ReferenceData {

    public static final int FRAUD_TOLERANCE = 30;

    public static final long DEFAULT_SUBWAY_FARE = 750;
    public static final long MINIMUM_SUBWAY_FARE = 150;
    public static final double MINIMUM_TRIP_FUDGE_FACTOR = 0.75;

    public static final String BUS_EVENT = "BUSTRIP";
    public static final String STARTSUBWAY_EVENT = "BEGINTRIP";
    public static final String ENDSUBWAY_EVENT = "ENDTRIP";

    public static final byte STATUS_OK = 1;
    public static final byte STATUS_NO_MONEY = 0;

    public static final byte STATUS_ON_ANOTHER_TRAIN = -1;
    public static final byte STATUS_MOVING_TOO_FAST = -2;
    public static final byte STATUS_KNOWN_FRAUD = -3;

    public static final byte STATUS_UNKNOWN_USER = -10;
    public static final byte STATUS_DUPLICATE_EVENT = -11;
    public static final byte STATUS_UNKNOWN_SUBSYSTEM = -12;
    public static final byte STATUS_UNKNOWN_EVENT = -13;
    public static final byte STATUS_BAD_FFARE = -14;
    public static final byte STATUS_BAD_DFARE = -15;
    public static final byte STATUS_NO_FININFO = -16;
    public static final byte STATUS_UNKNOWN_STATION = -17;
    public static final byte STATUS_NO_TRIP_STARTED = -18;
    public static final byte STATUS_UNKNOWN_BUSROUTE = -19;

    public static final String[] PRODUCTS = { "Bus & Tram Pass (Child Free)", "Bus & Tram Pass - B&T Discount-1 Month",
            "Bus & Tram Pass - B&T Discount-7 Day", "Bus & Tram Pass - B&T Discount-Period", "Bus & Tram Pass-1 Month",
            "Bus & Tram Pass-7 Day", "Bus & Tram Pass-Annual", "Bus & Tram Pass-Period", "Freedom Pass (Disabled)",
            "Freedom Pass (Elderly)", "LUL Travelcard-1 Month", "LUL Travelcard-7 Day", "LUL Travelcard->Annual",
            "LUL Travelcard-Annual", "LUL Travelcard-Period", "LUL Travelcard-Time Not Captured", "PAYG",
            "Staff Pass - Bus Operator", "Staff Pass - Bus Operator Nominee", "Staff Pass - Staff Nominee",
            "Staff Pass - Staff Retired including LCB", "Tfl Travel - Free" };

    public static final String[] BUSROUTES = { "1", "10", "100", "101", "102", "103", "104", "105", "106", "107", "108",
            "109", "11", "110", "111", "112", "113", "114", "115", "116", "117", "118", "119", "12", "120", "121",
            "122", "123", "124", "125", "126", "127", "128", "129", "13", "131", "132", "133", "134", "135", "136",
            "137", "138", "139", "14", "140", "141", "142", "143", "143D", "144", "145", "146", "147", "148", "149",
            "15", "150", "151", "152", "153", "154", "155", "156", "157", "158", "159", "15H", "16", "160", "161",
            "162", "163", "164", "165", "166", "167", "168", "169", "17", "170", "171", "172", "173", "174", "175",
            "176", "177", "178", "179", "18", "180", "181", "182", "183", "184", "185", "186", "187", "188", "189",
            "19", "190", "191", "192", "193", "194", "195", "196", "197", "198", "199", "2", "20", "200", "201", "202",
            "203", "204", "205", "206", "207", "208", "209", "21", "210", "211", "212", "213", "214", "215", "216",
            "217", "219", "22", "220", "221", "222", "223", "224", "225", "226", "227", "228", "229", "23", "230",
            "231", "232", "233", "234", "235", "236", "237", "238", "24", "240", "241", "242", "243", "244", "245",
            "246", "247", "248", "249", "25", "250", "251", "252", "253", "254", "255", "256", "257", "258", "259",
            "26", "260", "261", "262", "263", "264", "265", "266", "267", "268", "269", "27", "270", "271", "272",
            "273", "274", "275", "276", "277", "279", "28", "280", "281", "281R", "282", "283", "284", "285", "286",
            "287", "288", "289", "29", "290", "291", "292", "293", "294", "295", "296", "297", "298", "299", "2U", "3",
            "30", "300", "302", "303", "305", "307", "308", "309", "31", "312", "313", "315", "316", "317", "318",
            "319", "32", "320", "321", "322", "323", "325", "326", "327", "328", "329", "33", "330", "331", "332",
            "333", "336", "337", "339", "34", "340", "341", "343", "344", "345", "346", "347", "349", "35", "350",
            "352", "353", "354", "355", "356", "357", "358", "359", "36", "360", "362", "363", "364", "365", "366",
            "367", "368", "369", "37", "370", "370D", "371", "372", "375", "376", "377", "379", "38", "380", "381",
            "382", "383", "384", "385", "386", "387", "388", "389", "39", "390", "391", "393", "394", "395", "396",
            "397", "398", "399", "4", "40", "401", "403", "404", "405", "405D", "406", "407", "41", "410", "411", "412",
            "413", "414", "415", "417", "418", "419", "42", "422", "423", "424", "425", "427", "428", "43", "430",
            "432", "434", "436", "44", "440", "444", "45", "450", "452", "453", "455", "46", "460", "462", "463", "464",
            "465", "466", "467", "468", "469", "47", "470", "472", "473", "474", "476", "48", "481", "482", "484",
            "485", "486", "487", "488", "49", "490", "491", "492", "493", "496", "498", "499", "5", "50", "507", "51",
            "52", "521", "53", "54", "549", "55", "56", "57", "58", "59", "6", "60", "601", "602", "603", "605", "606",
            "607", "608", "609", "61", "611", "612", "613", "614", "616", "617", "62", "621", "624", "625", "626",
            "627", "628", "629", "63", "632", "634", "635", "636", "637", "638", "639", "64", "640", "641", "642",
            "643", "646", "647", "648", "649", "65", "650", "651", "652", "653", "654", "655", "656", "657", "658",
            "66", "660", "661", "664", "665", "667", "669", "66U", "67", "671", "672", "673", "674", "675", "678",
            "679", "68", "681", "683", "685", "686", "687", "688", "689", "69", "690", "691", "692", "696", "697",
            "698", "699", "7", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "8", "80", "81", "82", "83",
            "84", "85", "86", "86U", "87", "88", "89", "9", "90", "91", "92", "93", "94", "95", "953", "96", "969",
            "97", "98", "99", "9H", "A10", "B11", "B12", "B13", "B14", "B15", "B16", "C1", "C10", "C10D", "C11", "C2",
            "C3", "D3", "D6", "D7", "D8", "E1", "E10", "E11", "E2", "E3", "E5", "E6", "E7", "E8", "E9", "ELW", "G1",
            "H1", "H10", "H11", "H12", "H13", "H14", "H17", "H18", "H19", "H2", "H20", "H22", "H25", "H26", "H28", "H3",
            "H32", "H37", "H9", "H91", "H98", "K1", "K2", "K3", "K4", "K5", "LBN", "N1", "N10", "N102", "N105", "N108",
            "N11", "N111", "N119", "N12", "N128", "N13", "N133", "N134", "N136", "N137", "N139", "N14", "N140", "N148",
            "N149", "N15", "N155", "N159", "N16", "N171", "N176", "N18", "N188", "N189", "N19", "N2", "N20", "N205",
            "N207", "N21", "N213", "N214", "N22", "N220", "N23", "N236", "N24", "N242", "N243", "N25", "N250", "N253",
            "N26", "N264", "N266", "N27", "N271", "N274", "N277", "N279", "N28", "N281", "N285", "N29", "N295", "N297",
            "N3", "N31", "N321", "N341", "N343", "N344", "N345", "N35", "N36", "N365", "N369", "N37", "N38", "N381",
            "N390", "N41", "N43", "N44", "N453", "N47", "N472", "N474", "N5", "N52", "N53", "N55", "N550", "N551",
            "N57", "N6", "N63", "N65", "N68", "N69", "N7", "N72", "N73", "N74", "N76", "N8", "N83", "N85", "N86", "N87",
            "N88", "N89", "N9", "N91", "N93", "N94", "N97", "N98", "NC2", "P12", "P13", "P4", "P5", "PR2", "R1", "R10",
            "R11", "R2", "R3", "R4", "R5", "R6", "R68", "R7", "R70", "R8", "R9", "RV1", "S1", "S3", "S4", "T031",
            "T032", "T033", "T130", "T314", "U1", "U10", "U2", "U3", "U4", "U5", "U7", "U9", "W10", "W11", "W12", "W13",
            "W14", "W15", "W16", "W19", "W3", "W4", "W5", "W6", "W7", "W8", "W9", "X26", "X68" };

    public static final String[] STATIONS = { "ADDISCOMBE TRAM", "ADDNGTN VIL TRAM", "AMPERE WAY TRAM", "ARENA TRAM",
            "AVENUE ROAD TRAM", "Acton Central", "Acton Main Line", "Acton Town", "Aldgate", "Aldgate East",
            "All Saints", "Alperton", "Amersham", "Angel", "Archway", "Arnos Grove", "Arsenal", "BECKENHM JN TRAM",
            "BECKENHM RD TRAM", "BEDDNGTN LN TRAM", "BELGRAVE WK TRAM", "BIRKBECK TRAM", "Baker Street", "Balham",
            "Balham NR", "Balham SCL", "Bank", "Barbican", "Barking", "Barkingside", "Barons Court", "Battersea Park",
            "Bayswater", "Beckton", "Beckton Park", "Becontree", "Bellingham", "Belsize Park", "Bermondsey",
            "Bethnal Green", "Bethnal Green NR", "Blackfriars", "BLCKHRS LNE TRAM", "Blackhorse Road", "Blackwall",
            "Bond Street", "Borough", "Boston Manor", "Bounds Green", "Bow Church", "Bow Road", "Brent Cross",
            "Brixton", "Brockley", "Bromley By Bow", "Brondesbury", "Brondesbury Park", "Buckhurst Hill", "Burnt Oak",
            "Bus", "Bushey", "CHURCH STRT TRAM", "COOMBE LANE TRAM", "Caledonian Rd&B'sby", "Caledonian Road",
            "Cambridge Heath", "Camden Road", "Camden Town", "Canada Water", "Canary Wharf", "Canary Wharf DLR",
            "Canary Wharf E2", "Canning Town", "Cannon Street", "Canonbury", "Canons Park", "Carpenders Park",
            "Carshalton", "Castle Bar Park", "Chalfont & Latimer", "Chalk Farm", "Chancery Lane", "Charing Cross",
            "Chesham", "Chigwell", "Chiswick Park", "Chorleywood", "City Thameslink", "Clapham Common",
            "Clapham Junction", "Clapham North", "Clapham South", "Clapton", "Cockfosters", "Colindale",
            "Colliers Wood", "Covent Garden", "Crossharbour", "Crouch Hill", "Croxley", "Crystal Palace",
            "Custom House DLR", "Cutty Sark", "Cyprus", "DUNDONLD RD TRAM", "Dagenham Dock", "Dagenham East",
            "Dagenham Heathway", "Dalston Kingsland", "Debden", "Deptford Bridge", "Devons Road", "Dollis Hill",
            "Drayton Green", "Drayton Pk", "EAST CROYDON TRAM", "ELMERS END TRAM", "Ealing Broadway", "Ealing Common",
            "Earls Court", "East Acton", "East Croydon", "East Finchley", "East Ham", "East India", "East Putney",
            "Eastcote", "Edgware", "Edgware Road B", "Edgware Road M", "Elephant & Castle", "Elm Park", "Elverson Road",
            "Embankment", "Epping", "Essex Road", "Euston", "Euston NR", "Euston Square", "FENCHURCH ST NR",
            "FIELD WAY TRAM", "Fairlop", "Farringdon", "Fenchurch St NR", "Finchley Central", "Finchley Rd & Frognal",
            "Finchley Road", "Finsbury Park", "Forest Hill", "Fulham Broadway", "GEORGE STRT TRAM", "GRAVEL HILL TRAM",
            "Gallions Reach", "Gants Hill", "Gloucester Road", "Golders Green", "Goldhawk Road", "Goodge Street",
            "Gospel Oak", "Grange Hill", "Great Portland St", "Green Park", "Greenford", "Greenwich DLR", "Gunnersbury",
            "HARRNGTN RD TRAM", "Hackney Central", "Hackney Downs", "Hackney Wick", "Hainault", "Hammersmith D",
            "Hammersmith M", "Hampstead", "Hampstead Heath", "Hanger Lane", "Hanwell", "Harlesden",
            "Harringay Green Las", "Harrow On The Hill", "Harrow Wealdstone", "Hatch End", "Hatton Cross",
            "Hayes & Harlington", "Headstone Lane", "Heathrow Term 4", "Heathrow Term 5", "Heathrow Terms 123",
            "Harringay", "Hendon Central", "Heron Quays", "High Barnet", "High Street Kens", "Highbury", "Highgate",
            "Hillingdon", "Holborn", "Holland Park", "Holloway Road", "Homerton", "Honor Oak Park", "Hornchurch",
            "Hounslow Central", "Hounslow East", "Hounslow West", "Hyde Park Corner", "Ickenham", "Ilford",
            "Imperial Wharf", "Island Gardens", "KING HENRYS TRAM", "Kennington", "Kensal Green", "Kensal Rise",
            "Kensington Olympia", "Kentish Town", "Kentish Town West", "Kenton", "Kew Gardens", "Kilburn",
            "Kilburn High Road", "Kilburn Park", "King George V", "Kings Cross", "Kings Cross M", "Kings Cross T",
            "Kingsbury", "Knightsbridge", "LEBANON RD TRAM", "LLOYD PARK TRAM", "Ladbroke Grove", "Lambeth North",
            "Lancaster Gate", "Langdon Park", "Latimer Road", "Leicester Square", "Lewisham DLR", "Leyton",
            "Leyton Midland Road", "Leytonstone", "Leytonstone High Rd", "Limehouse DLR", "Limehouse NR",
            "Liverpool St NR", "Liverpool St WAGN TOC Gates", "Liverpool Street", "London Bridge",
            "London City Airport", "London Fields", "Loughton", "MERTON PARK TRAM", "MITCHAM JCN TRAM", "MITCHAM TRAM",
            "MORDEN ROAD TRAM", "Maida Vale", "Manor House", "Mansion House", "Marble Arch", "Marylebone",
            "Marylebone NR", "Mile End", "Mill Hill East", "Monument", "Moor Park", "Moorgate", "Morden",
            "Mornington Crescent", "Mudchute", "NEW ADDNGTH TRAM", "Neasden", "New Cross", "Newbury Park", "Norbury",
            "North Acton", "North Ealing", "North Greenwich", "North Harrow", "North Wembley", "Northfields",
            "Northolt", "Northolt Park", "Northwick Park", "Northwood", "Northwood Hills", "Norwood Junction SR",
            "Notting Hill Gate", "Oakwood", "Old Street", "Osterley", "Oval", "Oxford Circus", "PHIPPS BRDG TRAM",
            "Paddington", "Paddington FGW", "Park Royal", "Parsons Green", "Peckham Rye", "Perivale",
            "Piccadilly Circus", "Pimlico", "Pinner", "Plaistow", "Pontoon Dock", "Poplar", "Preston Road",
            "Prince Regent", "Pudding Mill Lane", "Purley", "Putney Bridge", "Queens Park", "Queensbury", "Queensway",
            "REEVES CRNR TRAM", "Rainham Essex", "Ravenscourt Park", "Rayners Lane", "Rectory Road", "Redbridge",
            "Regents Park", "Richmond", "Rickmansworth", "Roding Valley", "Romford", "Royal Albert", "Royal Oak",
            "Royal Victoria", "Ruislip", "Ruislip Gardens", "Ruislip Manor", "Russell Square", "SANDILANDS TRAM",
            "Seven Sisters", "Shadwell DLR", "Shepherd's Bush Mkt", "Shepherd's Bush NR", "Shepherd's Bush Und",
            "Sloane Square", "Snaresbrook", "South Acton", "South Croydon", "South Ealing", "South Greenford",
            "South Hampstead", "South Harrow", "South Kensington", "South Kenton", "South Quay", "South Ruislip",
            "South Tottenham", "South Wimbledon", "South Woodford", "Southall", "Southfields", "Southgate", "Southwark",
            "St James Street", "St James's Park", "St Johns Wood", "St Pancras International", "St Pauls",
            "Stamford Brook", "Stamford Hill", "Stanmore", "Stepney Green", "Stockwell", "Stoke Newington",
            "Stonebridge Park", "Stratford", "Streatham", "Streatham Common", "Sudbury Hill", "Sudbury Hill Harrow",
            "Sudbury Town", "Sudbury&Harrow Rd", "Sutton Surrey", "Swiss Cottage", "Sydenham SR", "TAMWORTH RD TRAM",
            "THERAPIA LN TRAM", "Temple", "Theydon Bois", "Thornton Heath", "Tooting Bec", "Tooting Broadway",
            "Tottenham Court Rd", "Tottenham Hale", "Totteridge", "Tower Gateway", "Tower Hill", "Tufnell Park",
            "Tulse Hill", "Turnham Green", "Turnpike Lane", "Upminster", "Upminster Bridge", "Upney", "Upper Holloway",
            "Upton Park", "Uxbridge", "Vauxhall", "Victoria", "Victoria TOCs", "WADDON MARSH TRAM", "WANDLE PARK TRAM",
            "WELLESLY RD TRAM", "WEST CROYDON TRAM", "WIMBLEDON TRAM", "WOODSIDE TRAM", "Wallington",
            "Walthamstow Central", "Walthamstow Qns R", "Wanstead", "Wanstead Park", "Warren Street", "Warwick Avenue",
            "Waterloo", "Waterloo JLE", "Watford High Street", "Watford Junction", "Watford Met", "Wembley Central",
            "Wembley Park", "Wembley Stadium", "West Acton", "West Brompton", "West Croydon", "West Drayton",
            "West Ealing", "West Finchley", "West Ham", "West Hampst'd NL", "West Hampst'd Tlink", "West Hampstead",
            "West Harrow", "West India Quay", "West Kensington", "West Norwood", "West Ruislip", "West Silvertown",
            "Westbourne Park", "Westferry", "Westminster", "White City", "Whitechapel", "Willesden Green",
            "Willesden Junction", "Wimbledon", "Wimbledon Park", "Wood Green", "Wood Lane", "Woodford",
            "Woodgrange Park", "Woodside Park", "Woolwich Arsenal DLR" };

}
