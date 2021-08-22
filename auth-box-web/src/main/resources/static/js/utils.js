function initUiTools() {
    setTimeout(function(){
        initTooltip();
        $('input[data-toggle="toggle"]').bootstrapToggle();
        $('.select2').select2();
    });
}

function initTooltip() {
//    $("[title]").tooltip({
//      classes: {
//        "ui-tooltip": "highlight"
//      }
//    });

//    $('.tooltip').not(this).hide();
}

function isEmpty(value) {
    if (value == undefined) {
        return true;
    } else if (value == null) {
        return true;
    } else if (value.length == 0) {
        return true;
    } else if (value == "") {
        return true;
    } else {
        return false;
    }
}

function isNotEmpty(value) {
    return !isEmpty(value);
}

function buttonLoading(querySelector) {
    $(querySelector).prop("disabled", true);
    $(querySelector).attr("original-text", $(querySelector).html());
    $(querySelector).attr("button-loading-state", "on");
    $(querySelector).html('<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> processing...');
}

function buttonReset(providedQuerySelector) {
    if (providedQuerySelector == undefined) {
        const querySelector = $("[button-loading-state='on']");
        $(querySelector).prop("disabled", false);
        $(querySelector).html($(querySelector).attr("original-text"));
    } else {
        $(providedQuerySelector).prop("disabled", false);
        $(providedQuerySelector).html($(providedQuerySelector).attr("original-text"));
    }
}

function paginationRange(min, max, currentPage) {
    let step = Math.floor((max - min) / 6);
    if (step < 1) {
        step = 1;
    }
    let input = [];
    let previousNumber = null;
    let currentPageAdded = false;
    for (let i = min; i <= max; i += step) {
        if (previousNumber != null && previousNumber < currentPage && currentPage < i) {
            input.push(currentPage);
            currentPageAdded = true;
        }
        if (currentPage === i) {
            currentPageAdded = true;
        }
        input.push(i);
        previousNumber = i;
    }
    if (!currentPageAdded) {
        input.push(currentPage);
    }
    if (input.indexOf(max) < 0) {
        input.push(max);
    }
    return input;
}

function uuidv4() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}