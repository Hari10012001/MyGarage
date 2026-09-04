import os

templates_dir = r'd:\HARIHARAN P\000_JAVA FULL STACK - Final Year Main_Projects - 2026_2027\019_MyGarage - A Vehicle Service History, Fuel Record and Maintenance Tracking Platform\backend\src\main\resources\templates'
nav_item = '            <li class="nav-item"><a class="nav-link sidebar-link" th:href="@{/vehicles/fuel-analytics}"><i class="bi bi-fuel-pump"></i> Fuel Intelligence</a></li>\n'

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    content = "".join(lines)
    if 'sidebar-link' in content and 'fuel-analytics' not in content:
        new_lines = []
        modified = False
        for line in lines:
            new_lines.append(line)
            if 'th:href="@{/vehicles}"' in line and '<li class="nav-item">' in line:
                new_lines.append(nav_item)
                modified = True
                
        if modified:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.writelines(new_lines)
            print(f'Modified: {filepath}')

for root, dirs, files in os.walk(templates_dir):
    for f in files:
        if f.endswith('.html'):
            process_file(os.path.join(root, f))
