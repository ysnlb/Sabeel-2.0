package com.example.domain.model

import java.util.UUID

object DhikrData {
    val ADHKAR_LIST = listOf(
        // Morning
        DhikrItem(
            id = UUID.randomUUID().toString(),
            category = DhikrCategory.MORNING,
            arabicText = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لاَ إِلَهَ إلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            translation = "We have entered a new morning and with it all dominion is Allah's. Praise is to Allah. None has the right to be worshipped but Allah alone, Who has no partner.",
            targetCount = 1
        ),
        DhikrItem(
            id = UUID.randomUUID().toString(),
            category = DhikrCategory.MORNING,
            arabicText = "اللَّهُمَّ عافِني في بَدَني، اللَّهُمَّ عافِني في سَمْعي، اللَّهُمَّ عافِني في بَصَري، لا إلهَ إلَّا أنتَ",
            translation = "O Allah, grant my body health. O Allah, grant my hearing health. O Allah, grant my sight health. None has the right to be worshipped but You.",
            targetCount = 3
        ),
        DhikrItem(
            id = UUID.randomUUID().toString(),
            category = DhikrCategory.MORNING,
            arabicText = "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ وَإِلَيْكَ النُّشُورُ",
            translation = "O Allah, by You we enter the morning and by You we enter the evening, by You we live and by You we die, and to You is the Final Return.",
            targetCount = 1
        ),
        DhikrItem(
            id = UUID.randomUUID().toString(),
            category = DhikrCategory.MORNING,
            arabicText = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
            translation = "Glory is to Allah and praise is to Him.",
            targetCount = 100
        ),
        
        // Evening
        DhikrItem(
            id = UUID.randomUUID().toString(),
            category = DhikrCategory.EVENING,
            arabicText = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لاَ إِلَهَ إلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            translation = "We have reached the evening and at this very time unto Allah belongs all sovereignty. Praise is to Allah. None has the right to be worshipped but Allah alone, Who has no partner.",
            targetCount = 1
        ),
        DhikrItem(
            id = UUID.randomUUID().toString(),
            category = DhikrCategory.EVENING,
            arabicText = "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ وَإِلَيْكَ الْمَصِيرُ",
            translation = "O Allah, by You we enter the evening and by You we enter the morning, by You we live and by You we die, and to You is the final return.",
            targetCount = 1
        ),
        DhikrItem(
            id = UUID.randomUUID().toString(),
            category = DhikrCategory.EVENING,
            arabicText = "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
            translation = "I seek the forgiveness of Allah and repent to Him.",
            targetCount = 100
        ),
        
        // After Prayer
        DhikrItem(
            id = UUID.randomUUID().toString(),
            category = DhikrCategory.AFTER_PRAYER,
            arabicText = "أَسْتَغْفِرُ اللَّهَ",
            translation = "I seek the forgiveness of Allah.",
            targetCount = 3
        ),
        DhikrItem(
            id = UUID.randomUUID().toString(),
            category = DhikrCategory.AFTER_PRAYER,
            arabicText = "اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ",
            translation = "O Allah, You are Peace and from You comes peace. Blessed are You, O Owner of majesty and honor.",
            targetCount = 1
        ),
        DhikrItem(
            id = UUID.randomUUID().toString(),
            category = DhikrCategory.AFTER_PRAYER,
            arabicText = "سُبْحَانَ اللَّهِ",
            translation = "Glory be to Allah",
            targetCount = 33
        ),
        DhikrItem(
            id = UUID.randomUUID().toString(),
            category = DhikrCategory.AFTER_PRAYER,
            arabicText = "الْحَمْدُ لِلَّهِ",
            translation = "Praise be to Allah",
            targetCount = 33
        ),
        DhikrItem(
            id = UUID.randomUUID().toString(),
            category = DhikrCategory.AFTER_PRAYER,
            arabicText = "اللَّهُ أَكْبَرُ",
            translation = "Allah is the Greatest",
            targetCount = 34
        )
    )
}
