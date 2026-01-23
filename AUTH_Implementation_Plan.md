# 🔐 Authentication Module Implementation Plan

This document outlines the technical plan to implement user authentication (Login) for the Pozitif E-İmza Launcher.

## 1. Objective
Enable users to log in using their email and password. valid credentials will grant access to the main application (JNLP Launcher). The system must also identify the device uniquely (`computerId`) to enforce license limits.

## 2. Architecture & Components

### A. Data Layer (Models)
We need Data Transfer Objects (DTOs) to map the JSON response from the API.

*   **`LoginResponse.java`**: Maps the main JSON response.
    *   `status` (String)
    *   `message` (String)
    *   `expireTime` (String)
    *   `userInfo` (Nested Object)
*   **`UserInfo.java`**: Maps the user details.
    *   `id`, `name`, `email`

### B. Service Layer (`AuthService.java`)
A dedicated service class responsible for:

1.  **Device Identification:**
    *   Generate a persistent `UUID` using `java.util.prefs.Preferences`.
    *   This ensures the `computerId` remains constant for the machine.
2.  **API Communication:**
    *   Use `Apache HttpClient` to send a `POST` request.
    *   Target URL: `https://eimza.globalpozitif.com.tr/?/webservice/member/post/checkLastOrderByEmailAndPassword`
    *   Headers: `Content-Type: application/json`
3.  **Data Parsing:**
    *   Use `Jackson ObjectMapper` to deserialize the JSON response into `LoginResponse` objects.

### C. UI Layer (`LoginView.java`)
A JavaFX UI component consisting of:
*   **Logo/Header:** "Pozitif E-İmza Giriş"
*   **Input Fields:**
    *   Email (`TextField`)
    *   Password (`PasswordField`)
*   **Action:** "Giriş Yap" Button.
*   **Feedback:** An error label to show authentication failures (e.g., "Hatalı şifre").

### D. Integration (`Main.java`)
The application flow will be updated:
1.  **Start:** Launch `LoginView`.
2.  **Interaction:** User clicks Login.
3.  **Process:** `AuthService` validates credentials.
4.  **Success:**
    *   Close Login View.
    *   Switch scene to `MainOrchestration` (Progress Bar & JNLP Launch).
5.  **Failure:** Stay on Login View and show error.

## 3. Implementation Steps

| Step | Task | Description |
| :--- | :--- | :--- |
| 1 | **Create Models** | Implement `LoginResponse` and `UserInfo` POJOs with Jackson annotations. |
| 2 | **Implement AuthService** | Create the `login(email, password)` method and `getComputerId()` logic. |
| 3 | **Design UI** | Create the Login form layout using JavaFX code (keeping it simple, no FXML needed for now to save complexity). |
| 4 | **Refactor Main** | Modify `start()` method to load Login logic first, then transition to `startOrchestration()`. |

## 4. Dependencies
*   **Existing:** `org.apache.httpcomponents.client5` (Http Client)
*   **Existing:** `com.fasterxml.jackson.databind` (JSON Parsing)
*   **Existing:** `javafx-controls` (UI)

No new Maven dependencies are required.

---
**Status:** Ready to Implement.
