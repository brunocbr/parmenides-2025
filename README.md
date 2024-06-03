# IAPS 8th Conference Book of Abstracts Generator

This repository contains the tools to generate the Book of Abstracts for the 8th Conference of the International Association for Presocratic Studies (IAPS).

## Requirements

To generate the Book of Abstracts, you will need the following tools installed on your system:

- [Babashka](https://github.com/babashka/babashka): A fast, expressive, and feature-rich shell/scripting environment for Clojure.
- [Pandoc](https://pandoc.org/): A universal document converter.
- LaTeX with XeLaTeX: For typesetting the PDF document.

## Setup

1. Clone this repository to your local machine.
2. Create a `.env` file in the root of the repository, exporting `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`, with your AWS credentials. Provide bucket and file name information with `AWS_S3_BUCKET_NAME` and `PDF_TARGET_NAME`.

### Google Drive Links

To generate the links to subfolders on a shared Drive folder, one per author name, ensure you have the following:

1. **Google Cloud Project**: Create a Google Cloud project, enable the Google Drive API, and create OAuth2 credentials.
2. **Generate `credentials.json`**:
   - Go to the [Google Cloud Console](https://console.cloud.google.com/).
   - Create a new project or select an existing project.
   - Navigate to the **API & Services > Credentials** page.
   - Click on **Create Credentials** and select **OAuth client ID**.
   - Configure the OAuth consent screen if prompted.
   - Choose **Desktop app** as the application type and click **Create**.
   - Download the `credentials.json` file and save it in the same directory as the script.
3. **Generate `token.json`**:
   - Go to the [OAuth2 Playground](https://developers.google.com/oauthplayground/).
   - In the "Step 1" box, find and select "Google Drive API v3", then check the scopes you need (e.g., `https://www.googleapis.com/auth/drive`).
   - Click **Authorize APIs** and sign in with your Google account.
   - In "Step 2", click **Exchange authorization code for tokens**.
   - Copy the access token and refresh token, and create a `token.json` file with the following structure:
     ```json
     {
       "access_token": "your-access-token",
       "refresh_token": "your-refresh-token",
       "token_type": "Bearer",
       "expires_in": 3599
     }
     ```
 4. Set `GOOGLE_DRIVE_FOLDER_ID` in your `.env` file to the corresponding folder id.

### Example `.env` file

```sh
# AWS Configuration
export AWS_DEFAULT_REGION=sa-east-1
export AWS_ACCESS_KEY_ID=<your access key>
export AWS_SECRET_ACCESS_KEY=<your secret>

# S3 Bucket Name
export AWS_S3_BUCKET_NAME=iaps-8

# Target PDF File Name
export PDF_TARGET_NAME=IAPS_8_Book_of_Abstracts.pdf

# Google Drive Folder ID
export GOOGLE_DRIVE_FOLDER_ID=<the folder id>
```

## Usage

To generate the Book of Abstracts in PDF format, use the following command:

```sh
make book
```

This command will compile the abstracts and generate the PDF file.

To upload the generated PDF to AWS S3, use the following command:

```sh
make deploy_pdf
```

This will upload the PDF to the specified AWS S3 bucket.

To create Google Drive subfolders using the author names:

```sh
make create_drive_subfolders
```

This will create the subfolders using the Google API.

To generate the links database for the author subfolders:

```sh
make generate_drive_links
```

This will create a database containing the links to the author subfolders, which will be used if available in the generation of the Book of Abstracts.

## Contributing

Contributions to this project are welcome. If you find any issues or have suggestions for improvements, please open an issue or submit a pull request.

---
Maintained by Bruno Conte
