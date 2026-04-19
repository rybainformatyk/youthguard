# 🛡️ YouthGuard - Twój Cyfrowy Anioł Stróż

**YouthGuard** to innowacyjna aplikacja na system Android, zaprojektowana z myślą o bezpieczeństwie dzieci i młodzieży. Aplikacja działa jako dyskretny system monitorowania rozmów telefonicznych, wykorzystujący sztuczną inteligencję (Speech-to-Text) do wykrywania potencjalnych zagrożeń w czasie rzeczywistym.

---

## 🚀 Kluczowe Funkcje

### 🎤 Monitoring Rozmów na Żywo
Aplikacja analizuje mowę podczas połączeń telefonicznych, przetwarzając ją na tekst. Działa w tle, zapewniając ciągłą ochronę bez przerywania rozmowy.

### 🔍 Inteligentna Analiza Zagrożeń
System automatycznie wyszukuje słowa kluczowe w czterech krytycznych kategoriach:
*   **Narkotyki i używki** (np. dopalacze, amfetamina).
*   **Samookaleczenia** (np. myśli samobójcze, przemoc wobec siebie).
*   **Nękanie (Bullying)** (np. zastraszanie, wyśmiewanie).
*   **Alkohol** (np. imprezy, nadużywanie substancji).

### 📲 System Alertów SMS
W momencie wykrycia niebezpieczeństwa, YouthGuard natychmiast wysyła wiadomość SMS do zdefiniowanych **Zaufanych Opiekunów**. SMS zawiera:
*   Informację o wykrytym zagrożeniu.
*   Fragment rozmowy (kontekst), w którym padło słowo.

### 🚨 Zdalna Interwencja
Opiekun może zareagować natychmiast. Odpisując na alert słowem **"POTWIERDZAM"**, zdalnie uruchamia na telefonie dziecka głośny alarm dźwiękowy i wizualny, mający na celu przerwanie niebezpiecznej sytuacji.

### 🌑 Interfejs Cyber-Refined
Nowoczesny wygląd typu **Amoled Black** z neonowymi akcentami. Interfejs jest nie tylko estetyczny, ale i czytelny, z wyraźną sygnalizacją stanu ochrony (Zielony - Aktywna / Czerwony - Wyłączona).

---

## 🛠️ Technologia

Aplikacja została zbudowana przy użyciu nowoczesnych technologii Android:
*   **Język:** Java
*   **Baza Danych:** SQLite (lokalna historia alertów i lista opiekunów).
*   **Speech-to-Text:** Android SpeechRecognizer API.
*   **Komunikacja:** BroadcastReceivers dla SMS i stanu połączeń.
*   **UI/UX:** Material Design 3, optymalizacja pod ekrany Amoled.

---

## 📖 Jak to działa?

1.  **Dodaj Opiekuna:** Wprowadź numer telefonu zaufanej osoby lub wybierz go z kontaktów.
2.  **Inicjuj Ochronę:** Kliknij przycisk na głównym ekranie. Dioda zmieni kolor na zielony.
3.  **Rozmawiaj bezpiecznie:** YouthGuard czuwa w tle. Jeśli rozmówca nie jest na liście zaufanych, system analizuje mowę.
4.  **Reaguj:** Jeśli opiekun otrzyma SMS, może zatwierdzić alarm, by pomóc dziecku.

---

## ⚠️ Uprawnienia

Dla poprawnego działania aplikacja wymaga:
*   `RECORD_AUDIO` – do analizy mowy.
*   `SEND_SMS` & `RECEIVE_SMS` – do wysyłania i odbierania alertów.
*   `READ_PHONE_STATE` – do wykrywania rozpoczęcia rozmowy.
*   `POST_NOTIFICATIONS` – do informowania o działaniu systemu.

---

*YouthGuard - Bo bezpieczeństwo Twojego dziecka jest najważniejsze.*
