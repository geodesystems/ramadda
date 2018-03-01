
function handlePsdStationClick (map, marker) {
    var entryId = marker.id;
    //Get the entry object, passing in the callback
    var entry = getRamadda().getEntry(entryId, handlePsdStationEntryClick);
    if(entry) {
        //If we got it right away then call the callback. Else the getEntry call above will call the callback
        handlePsdStationEntryClick(entry,map, marker);
    }
        
    //return false to not popup the window, true to popup as usual
    return false;
}


function handlePsdStationEntryClick(entry) {
    console.log("psd entry click:"+ entry.getId() +" " + entry.getName() + " " + entry.getAttributeValue("station_id"));
}
