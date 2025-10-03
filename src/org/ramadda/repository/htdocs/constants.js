//"use strict";
/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/



var ramaddaGlobals = {
    iconWidth:'18px'
}
var root = ramaddaBaseUrl;
var urlroot = ramaddaBaseUrl;


var ACTION_OK='ok';
var ACTION_NO='no';
var ACTION_CANCEL='cancel';

var LABEL_OK="OK";
var LABEL_YES="Yes";
var LABEL_NO="No";
var LABEL_CANCEL ="Cancel";

var ARG_ASCENDING='ascending';
var ARG_ORDERBY='orderby';
var ARG_PAGESEARCH='pagesearch';
var ARG_MAPBOUNDS='map_bounds';

var ICON_CLOSE = 'fas fa-window-close';
var ICON_STOP='fas fa-stop';
var ICON_PLAY='fas fa-play';
var ICON_CLOSE = 'fas fa-window-close';

var icon_pin = 'fas fa-thumbtack';
var icon_help = 'fas fa-question-circle';
var icon_command = ramaddaCdn + '/icons/command.png';
var icon_rightarrow = ramaddaCdn + '/icons/grayrightarrow.gif';
var icon_downdart = ramaddaCdn + '/icons/downdart.gif';
var icon_updart = ramaddaCdn + '/icons/updart.gif';
var icon_rightdart = ramaddaCdn + '/icons/rightdart.gif';
var icon_progress = ramaddaCdn + '/icons/progress.gif';
var icon_wait = ramaddaCdn + '/icons/wait.gif';
var icon_information = ramaddaCdn + '/icons/information.png';
var icon_folderclosed = ramaddaCdn + '/icons/folderclosed.png';
var icon_folderopen = ramaddaCdn + '/icons/togglearrowdown.gif';
var icon_folderclosed = 'fas fa-caret-right';
var icon_folderopen = 'fas fa-caret-down';
var icon_tree_open = ramaddaCdn + '/icons/togglearrowdown.gif';
var icon_tree_closed = ramaddaCdn + '/icons/togglearrowright.gif';
var icon_zoom = ramaddaCdn + '/icons/magnifier.png';
var icon_zoom_in = ramaddaCdn + '/icons/magnifier_zoom_in.png';
var icon_zoom_out = ramaddaCdn + '/icons/magnifier_zoom_out.png';
var icon_menuarrow = ramaddaCdn + '/icons/downdart.gif';
var icon_blank16 = ramaddaCdn + '/icons/blank16.png';
var icon_blank = ramaddaCdn + '/icons/blank.gif';
var icon_menu = ramaddaCdn + '/icons/menu.png';
var icon_trash =  'fas fa-trash-alt';

var UNIT_FT='ft';
var UNIT_MILES='mi';
var UNIT_KM='km';
var UNIT_M='m';
var UNIT_NM='nm';

var CLASS_FORMTABLE = 'formtable';
var CLASS_FORMLABEL = 'formlabel';
var CLASS_SEARCHABLE = 'ramadda-searchable';
var CLASS_BUTTON = 'ramadda-button';
var CLASS_MENU_ITEM = 'ramadda-menu-item';
var CLASS_DIALOG = 'ramadda-dialog';
var CLASS_DIALOG_BUTTON = 'ramadda-dialog-button';
var CLASS_COPYABLE = 'ramadda-copyable';
var CLASS_CLICKABLE = 'ramadda-clickable';
var CLASS_HIGHLIGHTABLE = 'ramadda-highlightable';
var CLASS_HOVERABLE = 'ramadda-hoverable';





var CURSOR_POINTER='pointer';
var CURSOR_DEFAULT='default';
var CURSOR_TEXT='text';
var CURSOR_CONTEXT_MENU = 'context-menu';

//Legacy IDs
var ID = 'id';
var BACKGROUND = 'background';
var CLASS = 'class';
var DIV = 'div';
var POSITION = 'position';
var WIDTH = 'width';
var ALIGN = 'align';
var VALIGN = 'valign';
var HEIGHT = 'height';
var SRC = 'src';
var STYLE = 'style';
var TABLE = 'table';
var TITLE = 'title';
var THEAD = 'thead';
var TBODY = 'tbody';
var TFOOT = 'tfoot';
var TR = 'tr';
var TD= 'td';
var BR= 'br';
var PRE = 'pre';
var SELECT = 'select';
var OPTION = 'option';
var VALUE = 'value';



var TAG_H1='h1';
var TAG_H2='h2';
var TAG_H3='h3';
var TAG_G='g';
var TAG_PRE= 'pre';
var TAG_BR= 'br';
var TAG_A = 'a';
var TAG_B = 'b';
var TAG_BODY = 'body';
var TAG_DIV = 'div';
var TAG_CANVAS = 'canvas';
var TAG_CENTER='center';
var TAG_I = 'i';
var TAG_IMG = 'img';
var TAG_IFRAME = 'iframe';
var TAG_INPUT = 'input';
var TAG_LABEL = 'label';
var TAG_LINK = 'link';
var TAG_LI = 'li';
var TAG_SCRIPT='script';
var TAG_SOURCE='source';
var TAG_RECT='rect';
var TAG_PATH='path';
var TAG_SVG = 'svg';
var TAG_SPAN = 'span';
var TAG_SELECT = 'select';
var TAG_OPTION = 'option';
var TAG_FORM = 'form';
var TAG_TABLE = 'table';
var TAG_TBODY = 'tbody';
var TAG_THEAD = 'thead';
var TAG_TH = 'th';
var TAG_TFOOT = 'tfoot';
var TAG_TR = 'tr';
var TAG_TD = 'td';
var TAG_UL = 'ul';
var TAG_OL = 'ol';

var ATTR_FOR='for';
var ATTR_AUTOFOCUS='autofocus';
var ATTR_ACTION= 'action';
var ATTR_BACKGROUND = 'background';


var ATTR_SLIDER_MIN='slider-min';
var ATTR_SLIDER_MAX='slider-max';
var ATTR_SLIDER_STEP='slider-step';
var ATTR_SLIDER_VALUE='slider-value';

var COLOR_TRANSPARENT='transparent';
var COLOR_BLACK='#000';
var COLOR_WHITE='#fff';
var COLOR_LIGHT_GRAY='#ccc';
var COLOR_MELLOW_YELLOW='var(--color-mellow-yellow)';


var ATTR_SRC = 'src';
var ATTR_ENTRYID='entryid';
var ATTR_TAG='tag';
var ATTR_TABLE_HEIGHT = 'table-height';
var ATTR_TABINDEX = 'tabindex';
var ATTR_TYPE = 'type';
var ATTR_TRANSFORM = 'transform';
var ATTR_LAYOUT='layout';
var ATTR_LOADING= 'loading';
var ATTR_DISABLED='disabled';

var ATTR_DATA_URL = 'data-url';
var ATTR_DATA_CORPUS='data-corpus';
var ATTR_DATA_TITLE='data-title';
var ATTR_DATA_ACTION='data-action';
var ATTR_DATA_MIN='data-min';
var ATTR_DATA_MAX='data-max';
var ATTR_DATA_VALUE='data-value';

var ATTR_IMGSRC='img-src';
var ATTR_WIDTH = 'width';
var ATTR_HEIGHT = 'height';
var ATTR_HREF = 'href';
var ATTR_ONMOUSEDOWN  = 'onmousedown';
var ATTR_ONCLICK  = 'onclick';
var ATTR_ONCHANGE  = 'onchange';
var ATTR_PLACEHOLDER = 'placeholder';
var ATTR_BORDER = 'border';
var ATTR_CANCEL='cancel';
var ATTR_CATEGORY = 'category';
var ATTR_CURSOR='cursor';
var ATTR_COLS = 'cols';
var ATTR_REL = 'rel';
var ATTR_READONLY='readonly';
var ATTR_ROWS='rows';
var ATTR_COLSPAN = 'colspan';
var ATTR_CELLPADDING = 'cellpadding';
var ATTR_CELLSPACING = 'cellspacing';
var ATTR_VALUE = 'value';
var ATTR_TITLE = 'title';
var ATTR_POSITION='position';
var ATTR_ALT = 'alt';
var ATTR_ID = 'id';
var ATTR_IDX = 'idx';
var ATTR_INDEX = 'index';
var ATTR_CLASS = 'class';
var ATTR_NAME = 'name';
var ATTR_NOWRAP = 'nowrap';
var ATTR_METHOD='method';
var ATTR_MULTIPLE = 'multiple';
var ATTR_SELECTED='selected';
var ATTR_SIZE = 'size';
var ATTR_STROKE = 'stroke';
var ATTR_STROKE_WIDTH = 'stroke-width';
var ATTR_FILL="fill";
var ATTR_STYLE = 'style';
var ATTR_TARGET = 'target';
var ATTR_ALIGN = 'align';
var ATTR_VALIGN = 'valign';
var SPACE = '&nbsp;';
var SPACE1 = '&nbsp;';
var SPACE2 = '&nbsp;&nbsp;';
var SPACE3 = '&nbsp;&nbsp;&nbsp;';
var SPACE4 = '&nbsp;&nbsp;&nbsp;&nbsp;';


var ALIGN_CENTER='center';
var ALIGN_TOP='top';
var ALIGN_BOTTOM='bottom';
var ALIGN_LEFT ='left';
var ALIGN_RIGHT ='right';


var POS_TOP='top';
var POS_LEFT='left';
var POS_BOTTOM='bottom';
var POS_RIGHT='right';

var POSITION_ABSOLUTE='absolute';
var POSITION_RELATIVE='relative';

var DISPLAY_NONE = 'none';
var DISPLAY_RELATIVE = 'relative';
var DISPLAY_BLOCK='block';
var DISPLAY_INLINE='inline';
var DISPLAY_INLINE_BLOCK='inline-block';

var OVERFLOW_NONE = 'none';
var OVERFLOW_AUTO = 'auto';
var OVERFLOW_HIDDEN = 'hidden';




var CSS_BASIC_BORDER='var(--basic-border)';

var FONT_BOLD='bold';
var FONT_ITALIC='italic';
var CSS_ALIGN_ITEMS='align-items';
var CSS_ALIGN='align';
var CSS_VISIBILITY = 'visibility';
var CSS_STROKE = 'stroke';
var CSS_STROKE_WIDTH = 'stroke-width';
var CSS_FILL='fill';
var CSS_FILL_OPACITY='fill-opacity';
var CSS_FONT_WEIGHT='font-weight';
var CSS_FONT_SIZE='font-size';
var CSS_FONT_STYLE='font-style';
var CSS_DISPLAY='display';
var CSS_TRANSFORM = 'transform';
var CSS_TEXT_ALIGN='text-align';
var CSS_TEXT_DECORATION='text-decoration';
var CSS_VERTICAL_ALIGN='vertical-align';
var CSS_OPACITY='opacity';
var CSS_OVERFLOW_Y='overflow-y';
var CSS_OVERFLOW_X='overflow-x';
var CSS_OVERFLOW_WRAP='overflow-wrap';
var CSS_MAX_HEIGHT='max-height';
var CSS_MIN_HEIGHT='min-height';
var CSS_MAX_WIDTH='max-width';
var CSS_MIN_WIDTH='min-width';
var CSS_MARGIN='margin';
var CSS_MARGIN_TOP='margin-top';
var CSS_MARGIN_BOTTOM='margin-bottom';
var CSS_MARGIN_LEFT='margin-left';
var CSS_MARGIN_RIGHT='margin-right';
var CSS_POINTER_EVENTS = 'pointer-events'
var CSS_PADDING='padding';
var CSS_PADDING_TOP='padding-top';
var CSS_PADDING_BOTTOM='padding-bottom';
var CSS_PADDING_LEFT='padding-left';
var CSS_PADDING_RIGHT='padding-right';
var CSS_POSITION='position';
var CSS_BORDER_COLLAPSE='border-collapse';
var CSS_BORDER_COLOR='border-color';
var CSS_BORDER_WIDTH='border-width';
var CSS_BORDER_SPACING='border-spacing';
var CSS_BORDER='border';
var CSS_BORDER_RADIUS='border-radius';
var CSS_BORDER_TOP='border-top';
var CSS_BORDER_RIGHT='border-right';
var CSS_BORDER_LEFT='border-left';
var CSS_BORDER_BOTTOM='border-bottom';
var CSS_Z_INDEX ='z-index';
var CSS_LINE_HEIGHT='line-height';
var CSS_LEFT='left';
var CSS_RIGHT='right';
var CSS_TOP='top';
var CSS_BOTTOM='bottom';
var CSS_CLIP_PATH='clip-path';
var CSS_COLOR='color';
var CSS_CURSOR = 'cursor';
var CSS_HEIGHT='height';
var CSS_WEBKIT_TRANSFORM="-webkit-transform";
var CSS_WHITE_SPACE='white-space';
var CSS_WIDTH='width';
var CSS_WORD_BREAK='word-break';
var CSS_BACKGROUND='background';
var CSS_BACKGROUND_IMAGE = 'background-image';
var CSS_BACKGROUND_REPEAT = 'background-repeat';
var CSS_BACKGROUND_COLOR='background-color';
var CSS_BACKGROUND_IMAGE='background-image';



