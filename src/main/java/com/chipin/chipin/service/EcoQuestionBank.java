package com.chipin.chipin.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The eco question bank. In every entry the FIRST answer is the correct one;
 * the quiz shuffles the answers before a member sees them.
 * Add a question by adding a line, then restart the app: new questions are added automatically.
 * Keep each question short, because members have only 10 seconds.
 */
public final class EcoQuestionBank {

    private EcoQuestionBank() {}

    public record Entry(String text, List<String> options, String explanation, int reward) {}

    // correct answer first, then 1 to 3 wrong answers
    private static Entry q(String text, int reward, String explanation, String correct, String... wrong) {
        List<String> options = new ArrayList<>();
        options.add(correct);
        options.addAll(Arrays.asList(wrong));
        return new Entry(text, options, explanation, reward);
    }

    public static List<Entry> all() {
        return List.of(
            q("Which of these is hardest to recycle?", 3,
              "Chip packets bond thin layers of plastic and foil together, and recyclers cannot separate them.",
              "A chip packet", "An aluminium can", "A glass jar", "Clean cardboard"),
            q("Which bulb uses the least electricity for the same brightness?", 2,
              "LEDs give the same light for a fraction of the power of the other bulbs.",
              "LED bulb", "CFL tube light", "Halogen bulb", "Filament bulb"),
            q("Besides carbon dioxide, which gas from rice fields and landfills traps heat?", 3,
              "Methane is released when waste and wet plant matter rot without air.",
              "Methane", "Nitrogen", "Oxygen", "Argon"),
            q("In 'reduce, reuse, recycle', what comes right after reduce?", 2,
              "Reusing an item keeps it working before it ever needs to be recycled.",
              "Reuse", "Recycle", "Burn", "Bury"),
            q("Which of these is NOT a renewable energy source?", 2,
              "Natural gas is a fossil fuel. It takes millions of years to form and releases carbon when burned.",
              "Natural gas", "Wind", "Hydropower", "Solar"),
            q("About how long can a plastic bottle last in the environment?", 3,
              "Plastic breaks into smaller and smaller pieces over hundreds of years but never truly disappears.",
              "Hundreds of years", "About 50 years", "About 10 years", "About 2 years"),
            q("Which habit saves the most water when washing clothes?", 2,
              "A full load uses far less water and power per garment than several part loads.",
              "Washing full loads", "Washing half loads", "Washing each shirt alone", "Rinsing every item first"),
            q("Why should used batteries not go in ordinary rubbish?", 3,
              "Batteries contain metals that can leak into soil and water, so they need a collection point.",
              "Their metals can leak into soil and water", "They give off methane", "They absorb moisture"),
            q("Which transport produces the least CO2 per passenger?", 2,
              "A full electric train carries many people on far less energy than the others.",
              "An electric train", "A petrol car with one person", "A short flight", "A diesel truck"),
            q("Which of these can go into a home compost heap?", 2,
              "Vegetable peelings and dry leaves rot into soil. Plastic, glass and diapers do not.",
              "Vegetable peelings and dry leaves", "Plastic wrapping", "Broken glass", "Dirty diapers"),
            q("Which gas do trees absorb from the air?", 2,
              "Trees take in carbon dioxide and store the carbon in wood and leaves.",
              "Carbon dioxide", "Methane", "Nitrogen", "Carbon monoxide"),
            q("What is 'standby power'?", 3,
              "Devices that are off but plugged in still draw a little electricity all day.",
              "Electricity used by switched-off devices that stay plugged in", "A backup generator", "Power saved at night",
              "Electricity stored in batteries"),
            q("Which of these breaks down naturally the fastest?", 2,
              "A banana peel rots in weeks. Plastic, aluminium and glass last far longer.",
              "A banana peel", "An aluminium can", "A plastic bag", "A glass bottle"),
            q("Which material can be recycled again and again without losing quality?", 3,
              "Glass can be melted and remade endlessly. Paper fibres weaken and plastic loses quality each time.",
              "Glass", "Paper", "Plastic bottles", "Foam packaging"),
            q("What is the best thing to do with a broken phone?", 2,
              "E-waste contains metals worth recovering and chemicals that must not reach soil or water.",
              "Hand it to an e-waste collection point", "Throw it in the general bin", "Burn it with other rubbish"),
            q("The Sundarbans mangrove forest mainly protects Bangladesh's coast from:", 3,
              "Mangroves break the force of storm surges and hold the shoreline together.",
              "Cyclone storm surges", "Earthquakes", "Landslides", "Droughts"),
            q("When is the best time to water garden plants to lose the least water?", 2,
              "In the cool early morning less water evaporates before it reaches the roots.",
              "Early morning", "Noon", "Mid-afternoon", "Only in winter"),
            q("Raising an air conditioner's setting by 1 degree typically saves roughly:", 3,
              "Each degree warmer cuts cooling electricity by around 3 to 5 percent.",
              "3 to 5 percent of its electricity", "20 percent of its electricity", "50 percent of its electricity", "Nothing"),
            q("A 'carbon footprint' measures:", 2,
              "It adds up the greenhouse gases caused by a person, product or activity.",
              "The greenhouse gases caused by an activity or person", "The carbon in your body", "The size of a forest",
              "The weight of waste you throw out"),
            q("Which food usually has the lowest carbon footprint per kilogram?", 3,
              "Lentils and other pulses need little land and energy compared with meat or farmed prawns.",
              "Lentils (dal)", "Beef", "Cheese", "Farmed prawns"),
            q("What saves the most resources when it comes to clothes?", 2,
              "Making clothes takes huge amounts of water and energy, so wearing them longer saves the most.",
              "Wearing them for longer", "Buying cheap new ones often", "Washing them hotter", "Tumble drying them"),
            q("Why is burning rubbish in the open harmful?", 2,
              "Open burning releases toxic smoke and fine particles that damage lungs.",
              "It releases toxic smoke and fine particles", "It cools the air", "It makes plastic safe"),
            q("Which fridge habit saves electricity?", 3,
              "Hot food makes the fridge work harder, so let it cool first and keep the door shut.",
              "Letting hot food cool before putting it in", "Setting it to the coldest level", "Leaving the door open while you choose",
              "Packing it so tightly that air cannot flow"),
            q("Which choice makes the least waste when buying drinks?", 2,
              "A refillable bottle replaces dozens of single-use bottles, cans or cartons.",
              "A refillable bottle", "Small plastic bottles in a pack", "Cartons with plastic straws", "Cans held with plastic rings"),
            q("Which way of drying clothes uses no electricity?", 2,
              "Sun and wind dry clothes for free, while dryers use a lot of power.",
              "Hanging them in the sun", "A tumble dryer on low", "A tumble dryer on high", "Ironing them while wet"),
            q("What are microplastics?", 3,
              "They are tiny plastic fragments, smaller than 5 mm, that spread through water, soil and food.",
              "Plastic fragments smaller than 5 mm", "Plastic made by bacteria", "Very thick plastic sheets", "Plastic that has been recycled"),
            q("The ozone layer protects living things from:", 3,
              "Ozone high in the atmosphere absorbs most of the Sun's harmful ultraviolet rays.",
              "Harmful ultraviolet rays", "Loud sounds", "Heavy rain", "Cyclones"),
            q("Why does cutting down forests increase carbon dioxide in the air?", 3,
              "Felled and burned trees release their stored carbon, and fewer trees remain to absorb CO2.",
              "Stored carbon is released and fewer trees absorb CO2", "Cut trees make oxygen only", "Rain stops falling over the sea"),
            q("Which saves the most trees?", 2,
              "Using both sides of a page and recycling it reduces the need for new wood pulp.",
              "Using both sides of paper and recycling it", "Using thicker paper", "Printing everything in colour", "Buying bleached paper"),
            q("Why is plastic in rivers especially harmful to fish?", 2,
              "Fish swallow it or get tangled in it, and the pieces then enter the food chain.",
              "They swallow it or get tangled, and it enters the food chain", "It makes the water too warm", "It makes the water sweeter")
        );
    }
}