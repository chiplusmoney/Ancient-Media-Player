import os
import shutil

OLD_PACKAGE = 'code.name.monkey.retromusic'
NEW_PACKAGE = 'player.music.ancient'

OLD_PATH = OLD_PACKAGE.replace('.', '/')
NEW_PATH = NEW_PACKAGE.replace('.', '/')

def replace_in_file(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except UnicodeDecodeError:
        return # Skip non-text files
    
    if OLD_PACKAGE in content or OLD_PATH in content:
        new_content = content.replace(OLD_PACKAGE, NEW_PACKAGE).replace(OLD_PATH, NEW_PATH)
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {file_path}")

def rename_directories(root_dir):
    for root, dirs, files in os.walk(root_dir, topdown=False):
        for d in dirs:
            if d == 'code' and 'name' in os.listdir(os.path.join(root, d)):
                # Found code directory which might contain name/monkey/retromusic
                old_full = os.path.join(root, 'code/name/monkey/retromusic')
                if os.path.exists(old_full):
                    # We have to move contents from old_full to the new path
                    new_base = os.path.join(root, 'player/music/ancient')
                    os.makedirs(new_base, exist_ok=True)
                    for item in os.listdir(old_full):
                        shutil.move(os.path.join(old_full, item), os.path.join(new_base, item))
                    print(f"Moved files from {old_full} to {new_base}")
                    # Clean up old empty directories
                    try:
                        os.rmdir(old_full)
                        os.rmdir(os.path.join(root, 'code/name/monkey'))
                        os.rmdir(os.path.join(root, 'code/name'))
                        os.rmdir(os.path.join(root, 'code'))
                    except OSError:
                        pass # Directory not empty

def main():
    root_dir = '/home/ntk/Desktop/Developer/Templates-Downloaded/RetroMusicPlayer-dev'
    for root, dirs, files in os.walk(root_dir):
        if '.git' in root or '.gradle' in root or 'build' in root or '.idea' in root:
            continue
        for file in files:
            if file.endswith(('.kt', '.java', '.xml', '.gradle', '.kts', '.pro', '.json', '.xml')):
                replace_in_file(os.path.join(root, file))
    
    # Rename directories in app/src/main/java, app/src/androidTest/java, etc.
    rename_directories(os.path.join(root_dir, 'app/src'))

if __name__ == '__main__':
    main()
