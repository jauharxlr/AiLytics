# R&D: AI-Powered Browser Automation System with Spring Boot

## Objective
Design a Spring Boot-based automation system capable of programmatically controlling a browser to perform complex tasks like navigation, authentication, form filling, and file uploads.

## Technical Approach & Library Research

### 1. Library Comparison
| Feature | **Playwright for Java** | **Selenium** | **Puppeteer (via JPP)** |
| :--- | :--- | :--- | :--- |
| **Performance** | High (Modern, fast) | Moderate (Industry standard) | High |
| **Reliability** | Auto-wait for elements | Manual waits often needed | High |
| **Multibrowser** | Chromium, WebKit, Firefox | All browsers | Primarily Chromium |
| **File Uploads** | Native and reliable | Supported | Supported |
| **Recommendation** | **Playwright** (Modern API, faster execution, and better element handling). |

### 2. Proposed Architecture
The system will follow a Producer-Consumer pattern to handle long-running browser tasks asynchronously.

#### **Components**:
1.  **REST API Controller**: Receives automation requests (URL, Credentials, Form Data, File).
2.  **Automation Service**: Orchestrates Playwright actions.
3.  **Job Manager**: Manages task status and results (using a database like H2/PostgreSQL).
4.  **Task Queue**: (Optional/Future) For high-concurrency, use Redis/RabbitMQ.

### 3. Workflow Detail

1.  **Trigger**: User sends a POST request to `/api/v1/automation/execute`.
2.  **Job Initialization**: A unique `jobId` is generated and returned to the user immediately.
3.  **Execution**:
    -   Launch headless/headed browser instance.
    -   Navigate to target URL.
    -   Find login elements (using AI-enhanced selectors or semantic matching).
    -   Perform Authentication.
    -   Fill forms using JSON data mapping.
    -   Handle file uploads via Playwright's `setInputFiles` method.
4.  **Reporting**: Final status and screenshots are saved.
5.  **Retrieval**: User polls `/api/v1/automation/status/{jobId}` to get the result.

## Implementation with Playwright
```java
try (Playwright playwright = Playwright.create()) {
    Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    Page page = browser.newPage();
    
    // 1. Navigate
    page.navigate("https://example.com/login");
    
    // 2. Authenticate
    page.fill("#username", "myUser");
    page.fill("#password", "myPass");
    page.click("button[type='submit']");
    
    // 3. Form Fill & File Upload
    page.fill("#form-data", "Some detailed info");
    page.setInputFiles("input[type='file']", Paths.get("upload.pdf"));
    
    // 4. Submit
    page.click("#submit-btn");
}
```

## AI Enhancement (Future-Proofing)
-   **Semantic Selectors**: Integrate Gemini to analyze the page DOM and suggest selectors for elements like "The login button" when IDs are dynamic.
-   **Error Recovery**: Use AI to interpret error messages on the page and attempt re-submission with corrected data.

## Next Steps
1.  Add `playwright` dependency to `pom.xml`.
2.  Implement the `AutomationService`.
3.  Create an `AutomationController` for asynchronous job management.
