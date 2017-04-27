/**
 * Created by dusanklinec on 30.01.17.
 */

/**
 * Switches main loading overlay.
 * @param started if true overlay is displayed. Hidden otherwise.
 */
function bodyProgress(started){
    var htmlBody = $("body");
    if (started){
        htmlBody.addClass("loading");
    } else {
        htmlBody.removeClass("loading");
    }

    return true;
}

/**
 * Shows confirmation prompt, if clicked yes, modal is started - progress indicator.
 * @param question
 */
function confirmAndModal(question){
    var confirmation = confirm(question);
    if (confirmation){
        bodyProgress(true);
    }

    return confirmation;
}

/**
 * Returns GET parameter
 * @param parameterName
 * @returns {*}
 */
function findGetParameter(parameterName) {
    var result = null,
        tmp = [];
    var items = location.search.substr(1).split("&");
    for (var index = 0; index < items.length; index++) {
        tmp = items[index].split("=");
        if (tmp[0] === parameterName) result = decodeURIComponent(tmp[1]);
    }
    return result;
}
