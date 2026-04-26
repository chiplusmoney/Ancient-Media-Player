import os

REPLACEMENTS = {
    'Hemanth Savarla': 'Future Eternal',
    'hemanthsavarla': 'futureeternal',
    'code_name_monkey': 'future_eternal',
    'Code Name Monkey': 'Future Eternal',
    'Code name monkey': 'Future Eternal',
    'hussain3112': 'futureeternal'
}

def replace_in_file(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except UnicodeDecodeError:
        return # Skip non-text files
    
    modified = False
    for old, new in REPLACEMENTS.items():
        if old in content:
            content = content.replace(old, new)
            modified = True
            
    if modified:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {file_path}")

def main():
    root_dir = '/home/ntk/Desktop/Developer/Templates-Downloaded/RetroMusicPlayer-dev'
    for root, dirs, files in os.walk(root_dir):
        if '.git' in root or '.gradle' in root or 'build' in root or '.idea' in root:
            continue
        for file in files:
            if file.endswith(('.kt', '.java', '.xml', '.gradle', '.kts', '.pro', '.json', '.md', '.txt')):
                replace_in_file(os.path.join(root, file))

if __name__ == '__main__':
    main()
