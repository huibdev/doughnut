import os
from openai import OpenAI
import time

# Initialize OpenAI client
client = OpenAI(api_key=os.getenv("OPENAI_API_TOKEN"))

def save_file_id(file_id, filename):
    with open(filename, 'w') as file:
        file.write(file_id)

def upload_file(filepath, purpose='assistants'):
    with open(filepath, 'rb') as file:
        response = client.files.create(file=file, purpose=purpose)
    return response.id

def attach_files_to_assistant(assistant_id, file_ids):
    # Add files to the assistant
    assistant = client.beta.assistants.update(
        assistant_id=assistant_id,
        file_ids=file_ids
    )
    return assistant

def find_source_files(root_dir):
    source_files = []
    for root, _, files in os.walk(root_dir):
        for file in files:
            if file.endswith(('.java', '.ts', '.vue')):
                source_files.append(os.path.join(root, file))
    return source_files

def main(assistant_id, project_dir):
    source_files = find_source_files(project_dir)
    file_ids = []

    for filepath in source_files:
        print(f'Uploading {filepath}...')
        file_id = upload_file(filepath)
        save_file_id(file_id, f'{os.path.basename(filepath)}_file_id.txt')
        file_ids.append(file_id)

    print('Attaching files to the assistant...')
    updated_assistant = attach_files_to_assistant(assistant_id, file_ids)
    print('Files successfully attached to the assistant:', updated_assistant.id)

# Set your assistant ID and project directory
assistant_id = 'your_assistant_id_here'
project_dir = 'path_to_your_project_directory'

if __name__ == "__main__":
    main(assistant_id, project_dir)
