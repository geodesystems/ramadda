import csv
import requests

# Define the list of tribes
tribes = [
    ["Washington", "Chehalis Tribe"],
    ["Washington", "Chinook Tribe"],
    ["Washington", "Confederated Tribes of the Colville Reservation"],
    ["Washington", "Confederated Tribes and Bands of the Yakama Nation"],
    ["Washington", "Cowlitz Tribe"],
    ["Washington", "Duwamish Tribe"],
    ["Washington", "Hoh Tribe"],
    ["Washington", "Jamestown S’Klallam Tribe"],
    ["Washington", "Kalispel Tribe"],
    ["Washington", "Lower Elwha Klallam Tribe"],
    ["Washington", "Lummi Nation"],
    ["Washington", "Makah Tribe"],
    ["Washington", "Muckleshoot Tribe"],
    ["Washington", "Nisqually Tribe"],
    ["Washington", "Nooksack Tribe"],
    ["Washington", "Port Gamble S’Klallam Tribe"],
    ["Washington", "Puyallup Tribe"],
    ["Washington", "Quileute Tribe"],
    ["Washington", "Quinault Nation"],
    ["Washington", "Samish Indian Nation"],
    ["Washington", "Sauk-Suiattle Tribe"],
    ["Washington", "Shoalwater Bay Tribe"],
    ["Washington", "Skokomish Tribe"],
    ["Washington", "Snohomish Tribe"],
    ["Washington", "Snoqualmie Tribe"],
    ["Washington", "Spokane Tribe"],
    ["Washington", "Squaxin Island Tribe"],
    ["Washington", "Steilacoom Tribe"],
    ["Washington", "Stillaguamish Tribe"],
    ["Washington", "Suquamish Tribe"],
    ["Washington", "Swinomish Tribe"],
    ["Washington", "Tulalip Tribes"],
    ["Washington", "Upper Skagit Tribe"],
    ["Oregon", "Burns Paiute Tribe"],
    ["Oregon", "Confederated Tribes of Coos, Lower Umpqua, and Siuslaw Indians"],
    ["Oregon", "Confederated Tribes of Grand Ronde"],
    ["Oregon", "Confederated Tribes of Siletz Indians"],
    ["Oregon", "Confederated Tribes of the Umatilla Indian Reservation"],
    ["Oregon", "Confederated Tribes of Warm Springs"],
    ["Oregon", "Coquille Tribe"],
    ["Oregon", "Cow Creek Band of Umpqua Tribe of Indians"],
    ["Oregon", "Klamath Tribes"],
    ["Montana", "Blackfeet Nation"],
    ["Montana", "Chippewa Cree Tribe of the Rocky Boy Reservation"],
    ["Montana", "Confederated Salish and Kootenai Tribes"],
    ["Montana", "Crow Tribe"],
    ["Idaho", "Coeur d'Alene Tribe"],
    ["Idaho", "Kootenai Tribe of Idaho"],
    ["Idaho", "Nez Perce Tribe"],
    ["Idaho", "Northwestern Band of Shoshone Nation"],
    ["Nevada", "Shoshone-Paiute Tribes of the Duck Valley Reservation"],
    ["Nevada", "Summit Lake Paiute Tribe"],
    ["California", "Hoopa Valley Tribe"],
    ["California", "Karuk Tribe"],
    ["California", "Tolowa Dee-ni' Nation (formerly Smith River Rancheria)"],
    ["California", "Yurok Tribe"],
    ["Alaska", "Metlakatla Indian Community"],
    ["Alaska", "Central Council of the Tlingit and Haida Indian Tribes of Alaska"],
    ["Alaska", "Organized Village of Kasaan"]
]

# Define a function to get the Wikipedia URL and coordinates for a tribe
def get_wikipedia_info(tribe_name):
    url = f"https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch={tribe_name}&format=json"
    response = requests.get(url).json()
    search_results = response['query']['search']
    if search_results:
        page_id = search_results[0]['pageid']
        page_title = search_results[0]['title']
#        wiki_url = f"https://en.wikipedia.org/?curid={page_id}"
        page_title = page_title.replace(' ','_');
        wiki_url = f"https://en.wikipedia.org/wiki/{page_title}"        
        print(wiki_url)
        
        # Fetch coordinates using Wikipedia API
        coords_url = f"https://en.wikipedia.org/w/api.php?action=query&prop=coordinates&pageids={page_id}&format=json"
        coords_response = requests.get(coords_url).json()
        coords = coords_response['query']['pages'][str(page_id)].get('coordinates', [{}])[0]
        lat = coords.get('lat', 'N/A')
        lon = coords.get('lon', 'N/A')
        
        return wiki_url, lat, lon
    return None, 'N/A', 'N/A'

# Add Wikipedia URL and coordinates to the list of tribes
tribes_info = []
for tribe in tribes:
    state, tribe_name = tribe
    wiki_url, lat, lon = get_wikipedia_info(tribe_name)
    tribes_info.append([state, tribe_name, wiki_url, lat, lon])

# Define the CSV file path
csv_file_path_detailed = "ATNI_member_tribes_detailed.csv"

# Write the detailed list to a CSV file
with open(csv_file_path_detailed, mode='w', newline='') as file:
    writer = csv.writer(file)
    writer.writerow(["State", "Tribe", "Wikipedia URL", "Latitude", "Longitude"])
    writer.writerows(tribes_info)

print(f"CSV file saved to {csv_file_path_detailed}")
