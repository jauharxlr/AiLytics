# SRS: AiLytics Portal Bridge Architecture

## 1. Overview
The "Portal Bridge" is the core orchestration layer of AiLytics. It seamlessly connects AI-driven document understanding with robotic browser automation to automate end-to-end business workflows (e.g., Insurance Claims, Invoice Processing).

## 2. Architecture Components

### 2.1 Generic Workflow Engine (`AutomationStep`)
Instead of hardcoded linear paths, AiLytics uses a **Recipe-based execution engine**.
- **Step Types**: `NAVIGATE`, `FILL_FORM`, `CLICK`, `UPLOAD_FILE`, `WAIT_FOR_LOAD`, `CAPTURE_RESULT`.
- **Wizard Support**: Steps can be chained to navigate through complex multi-page forms.
- **Data Mapping**: Fields extracted by Gemini are dynamically mapped to UI selectors at each step (including support for nested JSON paths).

### 2.2 Extraction Phase (`GeminiService`)
- Uses **Gemini 2.0 Flash**.
- Supports **Complex/Nested Schemas**: Can extract grouped data (e.g., `patient` info and `claim` details) to match multi-step forms.

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
