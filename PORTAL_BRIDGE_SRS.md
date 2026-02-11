# SRS: AiLytics Portal Bridge Architecture

## 1. Overview
The "Portal Bridge" is the core orchestration layer of AiLytics. It seamlessly connects AI-driven document understanding with robotic browser automation to automate end-to-end business workflows (e.g., Insurance Claims, Invoice Processing).

## 2. Architecture Components

### 2.1 Metadata Module (`MetadataService`)
Stores predefined configurations for specific business actions.
- **Action Name**: Unique identifier (e.g., `MEDISEP`).
- **Portal Configuration**: Login URLs, credential selectors, and submission endpoints.
- **Extraction Schema**: The exact JSON structure Gemini must produce from the document.
- **Field Mapping**: Maps JSON fields from Gemini to CSS selectors on the target portal.

### 2.2 Extraction Phase (`GeminiService`)
- Uses **Gemini 2.0 Flash** (multimodal).
- Accepts a document (PDF/Image) and the Action's JSON Schema.
- Produces a structured JSON object containing all required form data.

### 2.3 Automation Phase (`PortalBridgeService`)
- Powered by **Playwright**.
- Orchestrates the browser:
    1. Authenticates at the Portal Login.
    2. Navigates to the action page.
    3. Fills out forms using the extracted JSON data.
    4. Submits the form and captures the resulting transaction/unique ID.

### 2.4 Workflow Orchestrator (`WorkflowService`)
- Chains the Extraction and Automation phases.
- Manages asynchronous execution status.
- Provides a unified API for the user.

## 3. Reference Case: MEDISEP
- **Input**: Medical Bill (PDF).
- **Extraction**: Patient Name, Policy Number, Claim Amount, Date of Service.
- **Automation**: Logs into MEDISEP Portal, fills the claim form, and returns the **Claim ID**.

## 4. Future Readiness
The system is designed to swap **Gemini 2.0 Flash** for **Gemini 2.5 Flash** (or higher) via configuration, ensuring immediate access to improved reasoning and faster extraction as soon as the models are released.
