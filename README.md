# 🇪🇸 El País Opinion Scraper with Cross-Browser Automation via BrowserStack

This Java-based project scrapes the top opinion articles from [El País](https://elpais.com), translates their titles from Spanish to English, and runs in **parallel** across multiple browsers using **BrowserStack**. It’s ideal for showcasing **cross-browser testing**, **web scraping**, and **cloud-based parallel automation** in a real-world use case.

---

## 🔍 What This Project Does

✅ Visits [elpais.com](https://elpais.com) and navigates to the **"Opinión"** section  
✅ Scrapes the top **5 articles**  
✅ Extracts:
- Original Spanish title  
- Translated English title  
- First few paragraphs of content  
- First image (downloads it as `.jpg`)  
✅ Identifies **repeated words** in translated titles  
✅ Outputs everything to `elpais_output.csv`  
✅ Runs all scraping in **parallel on 5 browsers** via BrowserStack:
- Chrome (Windows 11)  
- Safari (macOS Ventura)  
- Firefox (Windows 10)  
- Chrome (Android)  
- Safari (iOS)

---

## 📦 Technologies Used

- Java 18
- Selenium WebDriver 4.21.0
- BrowserStack RemoteWebDriver
- Jsoup for HTML parsing
- Google Translate API via RapidAPI
- Maven for build management

---

## 🛠️ How to Run the Project

### 1️⃣ Prerequisites

- ✅ Java 18+ installed
- ✅ Maven installed
- ✅ BrowserStack account with username & access key
- ✅ RapidAPI account with a valid key for [google-translate113](https://rapidapi.com/developer/api/google-translate113)

---

### 2️⃣ Setup Instructions

1. **Clone or download** this project folder  
2. **Open `ElPaisScraper.java`**  
3. Replace the following placeholders with your credentials:

```java
private static final String USERNAME = "your_browserstack_username";
private static final String ACCESS_KEY = "your_browserstack_access_key";
private static final String TRANSLATE_API_KEY = "your_rapidapi_key";


### to run
 mvn clean compile exec:java

