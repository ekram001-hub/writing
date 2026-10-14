package com.example.data.demo

import com.example.data.model.ContentBlock
import com.example.data.model.ContentItem
import com.example.data.model.SectionPack
import com.example.data.model.TopicPack

object DemoTopicPack {
    val samplePack = TopicPack(
        schemaVersion = 1,
        topicId = "BB-AD-2026-FINTECH",
        revision = 1,
        title = "Central Bank Digital Currency & Cashless Economy in Bangladesh: Monetary Policy Challenges",
        titleBn = "বাংলাদেশে কেন্দ্রীয় ব্যাংক ডিজিটাল মুদ্রা (সিবিডিসি) ও ক্যাশলেস অর্থনীতি: মুদ্রানীতির চ্যালেঞ্জ",
        category = "Banking & Economy",
        tags = listOf("Bangladesh Bank", "Digital Finance", "Monetary Policy", "Financial Inclusion"),
        writingTypes = listOf("english_focus", "bangla_focus", "argument", "paragraph"),
        language = "mixed",
        difficulty = "intermediate",
        estimatedMinutes = 5,
        sections = listOf(
            SectionPack(
                sectionId = "overview",
                type = "overview",
                title = "Topic Overview & Exam Context",
                order = 1,
                durationSeconds = 60,
                content = listOf(
                    ContentBlock(
                        type = "paragraph",
                        text = "The transition towards a 'Smart Bangladesh' heavily relies on financial digitisation, mobile financial services (MFS), and interoperable payment systems. As Bangladesh Bank evaluates the feasibility of a Central Bank Digital Currency (CBDC) and advances the 'Binimoy' payment gateway and interoperable QR code (Bangla QR), candidates must critically examine both macroeconomic potential and liquidity risks."
                    ),
                    ContentBlock(
                        type = "paragraph",
                        text = "পরীক্ষার্থীদের জন্য বিশেষ নির্দেশনা: ফোকাস রাইটিংয়ে সরাসরি তাত্ত্বিক আলোচনার পরিবর্তে বাস্তব অর্থনৈতিক উপাত্ত, বাংলাদেশ ব্যাংকের নির্দেশিকা এবং সম্ভাব্য ঝুঁকিগুলোর কার্যকর সমাধান উপস্থাপন করলে বেশি নম্বর পাওয়া যায়।"
                    )
                )
            ),
            SectionPack(
                sectionId = "facts",
                type = "facts",
                title = "Verified Statistics & Institutional Sources",
                order = 2,
                durationSeconds = 90,
                items = listOf(
                    ContentItem(
                        id = "fact-001",
                        text = "Bangladesh MFS daily transactions surpassed BDT 4,000+ crore across over 220 million registered customer accounts.",
                        sourceName = "Bangladesh Bank Financial Inclusion Dept.",
                        publicationYear = 2025,
                        referenceDate = "2025-12-01"
                    ),
                    ContentItem(
                        id = "fact-002",
                        text = "Cashless retail transactions via Bangla QR grew over 180% year-on-year, significantly lowering micro-merchant transaction costs.",
                        sourceName = "Payment Systems Dept (PSD), Bangladesh Bank",
                        publicationYear = 2025
                    ),
                    ContentItem(
                        id = "fact-003",
                        text = "Currency in circulation (CIC) still constitutes approximately 11-13% of broad money (M2), indicating deep-rooted cash preference among marginalized rural sectors.",
                        sourceName = "BB Monetary Policy Statement (MPS)",
                        publicationYear = 2026
                    )
                )
            ),
            SectionPack(
                sectionId = "arguments",
                type = "arguments",
                title = "Core Arguments & Counterpoints",
                order = 3,
                durationSeconds = 90,
                items = listOf(
                    ContentItem(
                        id = "arg-001",
                        text = "Enhances Monetary Transmission: Direct digital currency circulation minimizes leakages in fiscal stimulus and social safety net programs.",
                        category = "Pro-Digitisation",
                        subtext = "মুদ্রানীতি প্রণয়ন ও বাস্তবায়নে বাংলাদেশ ব্যাংকের প্রত্যক্ষ নিয়ন্ত্রণ সুদৃঢ় হয়।"
                    ),
                    ContentItem(
                        id = "arg-002",
                        text = "Reduces Note-Printing & Logistics Costs: Physical currency minting, transportation, and destruction consume hundreds of crores annually.",
                        category = "Efficiency",
                        subtext = "কাগজের নোট মুদ্রণ ও নিরাপত্তা পরিবহনের বিপুল ব্যয় সাশ্রয় সম্ভব।"
                    ),
                    ContentItem(
                        id = "arg-003",
                        text = "Disintermediation Risk for Commercial Banks: Retail depositors might transfer commercial savings to risk-free central bank wallets during banking distress.",
                        category = "Monetary Risk",
                        subtext = "বাণিজ্যিক ব্যাংকের আমানত সংকট (Deposit Flight) এবং তারল্য সংকোচন ঝুঁকি তৈরি হতে পারে।"
                    ),
                    ContentItem(
                        id = "arg-004",
                        text = "Cybersecurity & Digital Divide: High vulnerability to cyber-heists and infrastructure disparity across rural union parishads.",
                        category = "Structural Challenge",
                        subtext = "সাইবার নিরাপত্তা ঝুঁকি এবং প্রান্তিক জনগোষ্ঠীর প্রযুক্তিগত অসমতা।"
                    )
                )
            ),
            SectionPack(
                sectionId = "vocabulary",
                type = "vocabulary",
                title = "High-Yield Banking Terminology",
                order = 4,
                durationSeconds = 60,
                items = listOf(
                    ContentItem(
                        id = "voc-001",
                        text = "Bank Disintermediation",
                        subtext = "ব্যাংক মধ্যস্থতাহীনতা (When funds bypass traditional commercial banks straight into central bank liabilities)"
                    ),
                    ContentItem(
                        id = "voc-002",
                        text = "Interoperable Payment Architecture",
                        subtext = "পারস্পরিক লেনদেনযোগ্য পেমেন্ট পরিকাঠামো (e.g., Bangla QR and National Payment Switch Bangladesh - NPSB)"
                    ),
                    ContentItem(
                        id = "voc-003",
                        text = "Currency in Circulation (CIC)",
                        subtext = "প্রচলনে থাকা কাগুজে মুদ্রা ও ধাতব মুদ্রা"
                    ),
                    ContentItem(
                        id = "voc-004",
                        text = "Prudential Liquidity Buffer",
                        subtext = "সতর্কতামূলক তারল্য তহবিল (SLR / CRR requirements to absorb unexpected shocks)"
                    )
                )
            ),
            SectionPack(
                sectionId = "structure",
                type = "structure",
                title = "Focus Writing Blueprint (5-Paragraph Framework)",
                order = 5,
                durationSeconds = 60,
                content = listOf(
                    ContentBlock(
                        type = "bullet",
                        text = "1. Punchy Hook: Define the paradigm shift from informal paper-ledger liquidity to sovereign digital ledgers in South Asia."
                    ),
                    ContentBlock(
                        type = "bullet",
                        text = "2. Economic Imperative: Cite MFS expansion, financial inclusion gains, and formalization of the informal economy."
                    ),
                    ContentBlock(
                        type = "bullet",
                        text = "3. Macro-Financial Vulnerabilities: Highlight bank run dynamics, commercial credit contraction, and digital fraud."
                    ),
                    ContentBlock(
                        type = "bullet",
                        text = "4. Policy Prescriptions: Tiered holding limits, two-tier intermediated model, and robust regulatory sandboxes."
                    ),
                    ContentBlock(
                        type = "bullet",
                        text = "5. Visionary Conclusion: How an agile central bank balances innovation with financial stability."
                    )
                )
            ),
            SectionPack(
                sectionId = "examples",
                type = "examples",
                title = "Sample Model Excerpt",
                order = 6,
                durationSeconds = 60,
                content = listOf(
                    ContentBlock(
                        type = "quote",
                        text = "“A Central Bank Digital Currency in Bangladesh should not be perceived merely as a digital alternative to banknotes, but as a sovereign liquidity architecture that bridges rural unbanked households with the formal banking spectrum. However, to prevent commercial bank disintermediation, Bangladesh Bank must mandate a two-tier distribution system with strict interest-free holding thresholds.”"
                    )
                )
            ),
            SectionPack(
                sectionId = "writing_task",
                type = "writing_task",
                title = "Notebook Writing Task (Timed)",
                order = 7,
                durationSeconds = 180,
                prompt = "Open your physical notebook. Write a cohesive 120-150 word focus paragraph analyzing: 'How can Bangladesh Bank balance the promotion of a cashless economy with the risk of commercial bank deposit flight?'\n\nAim for 1 topic sentence, 2 evidence sentences with BB statistics, 1 nuance/counter-point, and 1 policy conclusion."
            ),
            SectionPack(
                sectionId = "self_review",
                type = "self_review",
                title = "Post-Writing Self Checklist",
                order = 8,
                durationSeconds = 60,
                content = listOf(
                    ContentBlock(
                        type = "paragraph",
                        text = "Carefully read through your notebook writing and check off each evaluation criterion on the next screen."
                    )
                )
            ),
            SectionPack(
                sectionId = "notes",
                type = "notes",
                title = "Examiner's Evaluation Notes",
                order = 9,
                durationSeconds = 60,
                content = listOf(
                    ContentBlock(
                        type = "bullet",
                        text = "Penmanship & Speed: Ensure legible handwriting at an average speed of 25-30 words per minute under exam pressure."
                    ),
                    ContentBlock(
                        type = "bullet",
                        text = "Tone: Use formal central banking vocabulary (e.g., 'prudential regulation', 'monetary transmission mechanism', 'financial deepening')."
                    ),
                    ContentBlock(
                        type = "bullet",
                        text = "Avoid Generic Statements: Do not write vague platitudes like 'Technology is very good for Bangladesh'. Cite real frameworks (NPSB, Bangla QR, MFS)."
                    )
                )
            )
        )
    )
}
