import os
import requests
import pandas as pd

def read_ramadda_data(file_list, download_dir="."):
    """
    Downloads CSV files if they don't exist and loads them into a dictionary of DataFrames.
    
    Args:
        file_list (list): A list of dictionaries with 'url' and 'filename' keys.
        download_dir (str): Directory to save downloaded files.
    
    Returns:
        dict: A dictionary where keys are filenames and values are pandas DataFrames.
    """
    os.makedirs(download_dir, exist_ok=True)
    dataframes = {}

    for file in file_list:
        url = file["url"]
        filename = os.path.join(download_dir, file["filename"])
        
        # Download the file if it doesn't exist
        if not os.path.exists(filename):
            print(f"Downloading {url} to {filename}...")
            response = requests.get(url)
            response.raise_for_status()  # Raise an error for bad responses
            with open(filename, "wb") as f:
                f.write(response.content)
        # Load the CSV into a DataFrame
        dataframes[file["filename"]] = pd.read_csv(filename)
    
    return dataframes

files = [
    ${urls}	       
]

dataframes = read_ramadda_data(files)

