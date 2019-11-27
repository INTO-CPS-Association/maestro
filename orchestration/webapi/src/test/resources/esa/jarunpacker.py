from pathlib import Path
from zipfile import ZipFile


def extract_old_coe(jar, dest):
    with ZipFile(jar, 'r') as zipObj:
        # Get a list of all archived file names from the zip
        listOfFileNames = zipObj.namelist()
        # Iterate over the file names
        for fileName in listOfFileNames:
            # Check filename endswith csv
            if fileName.endswith('.jar'):
                # Extract a single file from zip
                # print(fileName)

                zipObj.extract(fileName, path=Path(dest))

    return True


def jar_unpacker(jar_file_path):
    directory = Path("tmp")
    directory = directory.resolve()
    if not directory.exists():
        directory.mkdir(parents=True)

    if not extract_old_coe(jar_file_path, directory):
        print ("Failed to extract jar")
        return None
    return directory
