# MicroWrite: Data Import & First-Launch Experience Plan

This plan addresses the questions: **"How does this app work?"** and **"How do I get data?"** by providing automatic preloading of curated BCS & Bank exam topics on initial launch, an in-app interactive tutorial guide in both Bengali and English, and enhanced data creation tools.

---

## 1. Context & Objectives

- **Current State:**
  - When the app is launched fresh, the database is empty (`totalTopicsCount == 0`).
  - Users are presented with an empty state requiring them to discover and tap "Load Bangladesh Bank Demo Pack" or manually paste/upload JSON in the Content Manager tab.
  - Users may not understand the 4-phase micro-writing methodology (Overview, Verified Facts, Argument Brainstorming, Timed Drafting) or how to bring their own topics.
- **Objectives:**
  - **Instant Usability:** Auto-seed a collection of high-quality exam writing topics (BCS, Bangladesh Bank AD, and general competitive exams) on the very first launch so users immediately see content without manual setup.
  - **Educational Guide:** Add an accessible, bilingual "How MicroWrite Works & How to Get Data" (কীভাবে কাজ করে ও ডেটা আনবেন) dialog/sheet right from the Home Screen and Content Manager.
  - **Topic Creation & Easy Import:** Add a streamlined in-app "Create Topic" visual form alongside the existing JSON import, plus sample template copying.

---

## 2. Proposed Changes

### Component: Data Layer & Preloaded Content
#### [data/demo/DemoTopicPack.kt] & [data/demo/TopicPacksProvider.kt]
- Expand the built-in topic packs to include multiple realistic Bangladesh exam questions:
  1. **Bangladesh Bank AD / Officers:** Central Bank Digital Currency (CBDC) & Cashless Economy in Bangladesh.
  2. **46th/47th BCS Focus Writing:** Climate-Resilient Agriculture & Food Security in Delta Plan 2100.
  3. **Economic Governance / Bank Exam:** Non-Performing Loans (NPL) Resolution & Financial Sector Stability in Bangladesh.
- Each pack includes bilingual titles, real institutional facts & statistics (Bangladesh Bank, BBS, BIDS), structured argument angles (pros, cons, structural challenges), and timed focus drafting prompts.

#### [data/repository/TopicRepository.kt] & [ui/feature/home/HomeViewModel.kt]
- Add an automatic initialization check on app start: if database topic count is 0 on launch, silently and reliably preload the starter topic packs.
- Provide a "Reset & Re-load Built-in Topics" option in Content Manager / Settings so users can always restore default data if cleared.

---

### Component: UI & In-App Guide
#### [ui/feature/home/HomeScreen.kt]
- Add an info card and "How to Use & Get Data" (সহায়িকা) button on the Home screen header.
- Provide a clean, friendly bottom sheet / dialog with 3 quick tabs:
  1. **কীভাবে কাজ করে (How it works):** Step-by-step breakdown of the 4 micro-learning phases (Context -> Verified Facts -> Arguments -> Timed Writing) with word counters and timer.
  2. **কীভাবে ডেটা আনবেন (How to get data):** Explanation of:
     - Built-in Curated Packs (এক ক্লিকে লোড)
     - JSON File Import (ফাইল সিলেক্ট করে ইম্পোর্ট)
     - Paste JSON from text (টেক্সট পেস্ট)
     - Topic Builder (নিজের তৈরি করা টপিক)
  3. **কাস্টম টপিক ফরম্যাট (Custom Format):** Visual schema explanation with a 1-click "Copy Sample JSON" button.

#### [ui/feature/contentmanager/ContentManagerScreen.kt]
- Add a new tab or action in Content Manager: **"Quick Topic Builder"** — allowing users to create a new writing prompt with title, category, facts, and prompt directly without writing JSON syntax.
- Add an explicit "Copy Sample JSON Template" button so users can copy the format to their clipboard and edit it in any text editor.

---

## 3. User Review Required

> [!NOTE]
> - **Default Preload Behavior:** Starter topics will be automatically inserted on the first launch so new users never face an empty screen. Users will still retain full control to delete, edit, or import custom packs.
> - **Language Support:** Explanations and guides will be in clear Bengali with English terminology (BCS, MFS, Focus Writing, etc.) to match the typical exam preparation workflow in Bangladesh.

---

## 4. Verification Plan

### Automated Build & Compilation
- Run `compile_applet` to ensure Room entities, database transactions, navigation callbacks, and Compose UI compile without error.

### Manual Verification Steps
1. **Fresh Database / First Launch:**
   - Launch the app with an empty database.
   - Verify that default starter topics appear on the Home screen immediately.
2. **"How to Use & Get Data" Guide:**
   - Tap the help/guide button on the Home screen and in Content Manager.
   - Verify all explanation tabs (Workflow, Data Import, Topic Template) display properly and have clear Bengali and English guidance.
3. **Topic Builder & Import:**
   - Test "Copy Sample JSON Template" to clipboard.
   - Test importing a custom JSON and adding a custom topic via the form.
4. **Practice Session Flow:**
   - Select one of the preloaded topics and verify all 4 phases (Overview, Facts, Brainstorming, Drafting) load and execute smoothly.
