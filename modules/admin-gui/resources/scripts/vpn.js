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

