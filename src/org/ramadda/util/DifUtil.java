/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.util;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class DifUtil {

    /** _more_ */
    public static final String HEADER_ARGS =
        "xmlns=\"\"\nxmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\nxsi:schemaLocation=\"http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/ http://gcmd.nasa.gov/Aboutus/xml/dif/dif_v9.7.1.xsd\">\n";


    /** _more_ */
    public static final String TAG_DIF = "DIF";

    /** _more_ */
    public static final String TAG_Entry_ID = "Entry_ID";

    /** _more_ */
    public static final String TAG_Entry_Title = "Entry_Title";

    /** _more_ */
    public static final String TAG_ISO_Topic_Category = "ISO_Topic_Category";

    /** _more_ */
    public static final String TAG_Data_Set_Citation = "Data_Set_Citation";

    /** _more_ */
    public static final String TAG_Dataset_Creator = "Dataset_Creator";

    /** _more_ */
    public static final String TAG_Dataset_Title = "Dataset_Title";

    /** _more_ */
    public static final String TAG_Dataset_Series_Name =
        "Dataset_Series_Name";

    /** _more_ */
    public static final String TAG_Dataset_Release_Date =
        "Dataset_Release_Date";

    /** _more_ */
    public static final String TAG_Dataset_Release_Place =
        "Dataset_Release_Place";

    /** _more_ */
    public static final String TAG_Dataset_Publisher = "Dataset_Publisher";

    /** _more_ */
    public static final String TAG_Version = "Version";

    /** _more_ */
    public static final String TAG_Issue_Identification =
        "Issue_Identification";

    /** _more_ */
    public static final String TAG_Data_Presentation_Form =
        "Data_Presentation_Form";

    /** _more_ */
    public static final String TAG_Other_Citation_Details =
        "Other_Citation_Details";

    /** _more_ */
    public static final String TAG_Online_Resource = "Online_Resource";


    /** _more_ */
    public static final String[] TAGS_Data_Set_Citation = {
        TAG_Dataset_Creator, TAG_Dataset_Title, TAG_Dataset_Series_Name,
        TAG_Dataset_Release_Date, TAG_Dataset_Release_Place,
        TAG_Dataset_Publisher, TAG_Version, TAG_Issue_Identification,
        TAG_Data_Presentation_Form, TAG_Other_Citation_Details,
        TAG_Online_Resource,
    };




    /** _more_ */
    public static final String TAG_Personnel = "Personnel";

    /** _more_ */
    public static final String TAG_Role = "Role";

    /** _more_ */
    public static final String TAG_First_Name = "First_Name";

    /** _more_ */
    public static final String TAG_Middle_Name = "Middle_Name";

    /** _more_ */
    public static final String TAG_Last_Name = "Last_Name";

    /** _more_ */
    public static final String TAG_Email = "Email";

    /** _more_ */
    public static final String TAG_Phone = "Phone";

    /** _more_ */
    public static final String TAG_Fax = "Fax";



    //    public static final String[] TAGS_Personnel = {


    /** _more_ */






    /** _more_ */
    public static final String TAG_Contact_Address = "Contact_Address";

    /** _more_ */
    public static final String TAG_Address = "Address";

    /** _more_ */
    public static final String TAG_City = "City";

    /** _more_ */
    public static final String TAG_Province_or_State = "Province_or_State";

    /** _more_ */
    public static final String TAG_Postal_Code = "Postal_Code";

    /** _more_ */
    public static final String TAG_Country = "Country";


    /** _more_ */
    public static final String TAG_Discipline = "Discipline";

    /** _more_ */
    public static final String TAG_Discipline_Name = "Discipline_Name";

    /** _more_ */
    public static final String TAG_Subdiscipline = "Subdiscipline";

    /** _more_ */
    public static final String TAG_Detailed_Subdiscipline =
        "Detailed_Subdiscipline";


    /** _more_ */
    public static final String TAG_Parameters = "Parameters";

    /** _more_ */
    public static final String TAG_Category = "Category";

    /** _more_ */
    public static final String TAG_Topic = "Topic";

    /** _more_ */
    public static final String TAG_Term = "Term";

    /** _more_ */
    public static final String TAG_Variable_Level_1 = "Variable_Level_1";

    /** _more_ */
    public static final String TAG_Variable_Level_2 = "Variable_Level_2";

    /** _more_ */
    public static final String TAG_Variable_Level_3 = "Variable_Level_3";

    /** _more_ */
    public static final String TAG_Detailed_Variable = "Detailed_Variable";

    /** _more_ */
    public static final String[] TAGS_Parameters = {
        TAG_Parameters, TAG_Category, TAG_Topic, TAG_Term,
        TAG_Variable_Level_1, TAG_Variable_Level_2, TAG_Variable_Level_3
    };


    /** _more_ */
    public static final String TAG_Keyword = "Keyword";

    /** _more_ */
    public static final String TAG_Sensor_Name = "Sensor_Name";

    /** _more_ */
    public static final String TAG_Short_Name = "Short_Name";

    /** _more_ */
    public static final String TAG_Long_Name = "Long_Name";

    /** _more_ */
    public static final String TAG_Source_Name = "Source_Name";


    /** _more_ */
    public static final String[] TAGS_Sensor_Name = { TAG_Short_Name,
            TAG_Long_Name };

    /** _more_ */
    public static final String[] TAGS_Project = { TAG_Short_Name,
            TAG_Long_Name };

    /** _more_ */
    public static final String[] TAGS_Source_Name = { TAG_Short_Name,
            TAG_Long_Name };



    /** _more_ */
    public static final String TAG_Temporal_Coverage = "Temporal_Coverage";

    /** _more_ */
    public static final String TAG_Start_Date = "Start_Date";

    /** _more_ */
    public static final String TAG_Stop_Date = "Stop_Date";

    /** _more_ */
    public static final String TAG_Paleo_Temporal_Coverage =
        "Paleo_Temporal_Coverage";

    /** _more_ */
    public static final String TAG_Paleo_Start_Date = "Paleo_Start_Date";

    /** _more_ */
    public static final String TAG_Paleo_Stop_Date = "Paleo_Stop_Date";

    /** _more_ */
    public static final String TAG_Chronostratigraphic_Unit =
        "Chronostratigraphic_Unit";

    /** _more_ */
    public static final String TAG_Eon = "Eon";

    /** _more_ */
    public static final String TAG_Era = "Era";

    /** _more_ */
    public static final String TAG_Period = "Period";

    /** _more_ */
    public static final String TAG_Epoch = "Epoch";

    /** _more_ */
    public static final String TAG_Stage = "Stage";

    /** _more_ */
    public static final String TAG_Detailed_Classification =
        "Detailed_Classification";



    /** _more_ */
    public static final String TAG_Data_Set_Progress = "Data_Set_Progress";

    /** _more_ */
    public static final String TAG_Spatial_Coverage = "Spatial_Coverage";

    /** _more_ */
    public static final String TAG_Southernmost_Latitude =
        "Southernmost_Latitude";

    /** _more_ */
    public static final String TAG_Northernmost_Latitude =
        "Northernmost_Latitude";

    /** _more_ */
    public static final String TAG_Westernmost_Longitude =
        "Westernmost_Longitude";

    /** _more_ */
    public static final String TAG_Easternmost_Longitude =
        "Easternmost_Longitude";

    /** _more_ */
    public static final String TAG_Minimum_Altitude = "Minimum_Altitude";

    /** _more_ */
    public static final String TAG_Maximum_Altitude = "Maximum_Altitude";

    /** _more_ */
    public static final String TAG_Minimum_Depth = "Minimum_Depth";

    /** _more_ */
    public static final String TAG_Maximum_Depth = "Maximum_Depth";

    /** _more_ */
    public static final String TAG_Location = "Location";

    /** _more_ */
    public static final String TAG_Location_Category = "Location_Category";

    /** _more_ */
    public static final String TAG_Location_Type = "Location_Type";

    /** _more_ */
    public static final String TAG_Location_Subregion1 =
        "Location_Subregion1";

    /** _more_ */
    public static final String TAG_Location_Subregion2 =
        "Location_Subregion2";

    /** _more_ */
    public static final String TAG_Location_Subregion3 =
        "Location_Subregion3";

    /** _more_ */
    public static final String TAG_Detailed_Location = "Detailed_Location";


    /** _more_ */
    public static final String[] TAGS_Location = { TAG_Location_Type,
            TAG_Location_Subregion1, TAG_Location_Subregion2,
            TAG_Location_Subregion3, TAG_Detailed_Location };



    /** _more_ */
    public static final String TAG_Data_Resolution = "Data_Resolution";

    /** _more_ */
    public static final String TAG_Latitude_Resolution =
        "Latitude_Resolution";

    /** _more_ */
    public static final String TAG_Longitude_Resolution =
        "Longitude_Resolution";

    /** _more_ */
    public static final String TAG_Horizontal_Resolution_Range =
        "Horizontal_Resolution_Range";

    /** _more_ */
    public static final String TAG_Vertical_Resolution =
        "Vertical_Resolution";

    /** _more_ */
    public static final String TAG_Vertical_Resolution_Range =
        "Vertical_Resolution_Range";

    /** _more_ */
    public static final String TAG_Temporal_Resolution =
        "Temporal_Resolution";

    /** _more_ */
    public static final String TAG_Temporal_Resolution_Range =
        "Temporal_Resolution_Range";


    /** _more_ */
    public static final String TAG_Project = "Project";


    /** _more_ */
    public static final String TAG_Quality = "Quality";

    /** _more_ */
    public static final String TAG_Access_Constraints = "Access_Constraints";

    /** _more_ */
    public static final String TAG_Use_Constraints = "Use_Constraints";

    /** _more_ */
    public static final String TAG_Data_Set_Language = "Data_Set_Language";

    /** _more_ */
    public static final String TAG_Originating_Center = "Originating_Center";

    /** _more_ */
    public static final String TAG_Data_Center = "Data_Center";

    /** _more_ */
    public static final String TAG_Data_Center_Name = "Data_Center_Name";


    /** _more_ */
    public static final String TAG_Data_Center_URL = "Data_Center_URL";

    /** _more_ */
    public static final String TAG_Data_Set_ID = "Data_Set_ID";


    /** _more_ */
    public static final String TAG_Distribution = "Distribution";


    /** _more_ */
    public static final String TAG_Distribution_Media = "Distribution_Media";

    /** _more_ */
    public static final String TAG_Distribution_Size = "Distribution_Size";

    /** _more_ */
    public static final String TAG_Distribution_Format =
        "Distribution_Format";

    /** _more_ */
    public static final String TAG_Distribution_Fees = "Fees";

    /** _more_ */
    public static final String[] TAGS_Distribution = { TAG_Distribution_Media,
            TAG_Distribution_Size, TAG_Distribution_Format,
            TAG_Distribution_Fees };


    /** _more_ */
    public static final String TAG_Multimedia_Sample = "Multimedia_Sample";

    /** _more_ */
    public static final String TAG_File = "File";

    /** _more_ */
    public static final String TAG_Format = "Format";

    /** _more_ */
    public static final String TAG_Caption = "Caption";

    /** _more_ */
    public static final String TAG_Description = "Description";

    /** _more_ */
    public static final String TAG_Reference = "Reference";

    /** _more_ */
    public static final String TAG_Summary = "Summary";

    /** _more_ */
    public static final String TAG_Related_URL = "Related_URL";

    /** _more_ */
    public static final String TAG_URL_Content_Type = "URL_Content_Type";

    /** _more_ */
    public static final String TAG_Type = "Type";

    /** _more_ */
    public static final String TAG_Subtype = "Subtype";




    /** _more_ */
    public static final String TAG_URL = "URL";


    /** _more_ */
    public static final String[] TAGS_Related_URL = { TAG_URL,
            TAG_Description };



    /** _more_ */
    public static final String TAG_Parent_DIF = "Parent_DIF";

    /** _more_ */
    public static final String TAG_IDN_Node = "IDN_Node";


    /** _more_ */
    public static final String TAG_Originating_Metadata_Node =
        "Originating_Metadata_Node";

    /** _more_ */
    public static final String TAG_Metadata_Name = "Metadata_Name";

    /** _more_ */
    public static final String TAG_Metadata_Version = "Metadata_Version";

    /** _more_ */
    public static final String TAG_DIF_Creation_Date = "DIF_Creation_Date";

    /** _more_ */
    public static final String TAG_Last_DIF_Revision_Date =
        "Last_DIF_Revision_Date";

    /** _more_ */
    public static final String TAG_DIF_Revision_History =
        "DIF_Revision_History";

    /** _more_ */
    public static final String TAG_Future_DIF_Review_Date =
        "Future_DIF_Review_Date";

    /** _more_ */
    public static final String TAG_Private = "Private";

}
