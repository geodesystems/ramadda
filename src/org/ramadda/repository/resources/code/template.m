% Define the list of URLs and filenames
urls = { ...
	 ${urls}
};

% Initialize an array to hold the data
ramaddaData = struct();

% Loop through the list of URLs and filenames
for i = 1:size(urls, 1)
    url = urls{i, 1};
    filename = urls{i, 2};
    
    % Check if the file already exists
    if ~isfile(filename)
        fprintf('Downloading %s...\n', filename);
        try
            websave(filename, url);
        catch ME
            fprintf('Failed to download %s: %s\n', filename, ME.message);
            continue;
        end
    end
    
    % Load the CSV data into the struct
    try
        data = readtable(filename);
        ramaddaData(i).Filename = filename;
        ramaddaData(i).Data = data;
    catch ME
        fprintf('Failed to load %s: %s\n', filename, ME.message);
    end
end


