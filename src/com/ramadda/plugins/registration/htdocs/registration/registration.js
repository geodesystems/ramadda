

function registrationUserChanged() {
    var perUser = 60;
    if (!(typeof pricePerUser === 'undefined')) {
        perUser = pricePerUser;
    }

    var users =$("#users").val();
    if(users == null || users == "") {
        return;
    }
    var cost = users*perUser;
    if(cost>0) {
        $("#amount").html(cost +" USD");
    }  else {
        $("#amount").html("");
    }
}


$("#users").change(function() {
        registrationUserChanged();
    });

$("#users").blur(function() {
        registrationUserChanged();
    });


/*
$("#amount").change(function() {
        $("#users").val("");
});
*/