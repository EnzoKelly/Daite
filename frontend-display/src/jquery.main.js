/**
 * Created by Artur on 09.08.2016.
 */

(function ($) {

    /* Variables */
    var my_username, my_age, my_gender, 
        match_username,

    // Typing variables
        typing = false,
        timeout = undefined,

    //  socket = io.connect("http://duk.im:6969/chat");
        socket = io.connect("http://localhost:6969/chat");


    /* ON LOAD */
    $(document).ready(function () {

        // Brings focus to name entry text box on page load
        var name = $("#name");
        name.focus();

        // Define the dialogs
        var content = $('.content'),
            warning_dialog = $('#warning-dialog'),
            yes_no_dialog = $('#yes-no-dialog'),
            no_match_dialogue = $('#no-match-dialog');
        
        warning_dialog.dialog({
            autoOpen: false,
            draggable: false,
            resizable: false,
            modal: true,
            show: {effect: 'fade', duration: 500},
            hide: {effect: 'fade', duration: 500},
            position: {my: 'center', of: content, within: content},
            buttons: [
                {
                    text: "Okay",
                    click: function () {
                        $(this).dialog('close');
                    }
                }
            ]
        });

        yes_no_dialog.dialog({
            autoOpen: false,
            draggable: false,
            resizable: false,
            modal: true,
            show: {effect: 'fade', duration: 500},
            hide: {effect: 'fade', duration: 500},
            position: {my: 'center', of: content, within: content},
            buttons: [
                {
                    text: "Yes",
                    click: function () {
                        $(this).dialog('close');
                    }
                },
                {
                    text: "No",
                    click: function () {
                        $(this).dialog('close');
                    }
                }
            ]
        });

        // Button tooltips
        var name_continue = $('#name-continue'),
            details_continue = $('#details-continue');

        name_continue.tooltip({
            position: {my: "left+10% center", at: "right center", of: name_continue, within: content}
        });

        details_continue.tooltip({
            position: {my: "left+10% center", at: "right center", of: details_continue, within: content}
        });

        // On keypress
        $('#type-message').keydown(function (e) {
            // If key isn't enter, set typing to true and tell the server
            if (e.which != 13) {
                if (typing === false) {
                    typing = true;
                    socket.emit("isTyping", true);
                }
                else {
                    // Checks if typing every second
                    clearTimeout(timeout);
                    timeout = setTimeout(typingTimeout, 1000);
                }
            }
            // If key is enter, send message
            else if (e.which == 13) {
                sendMessage();
            }
        });

        name.on("keyup", function () {
            if (name.val())
                $('#name-continue').tooltip('option', 'disabled', true);
            else
                $('#name-continue').tooltip('option', 'disabled', false);
        });

        $('#age').on('keyup', enter_details_validate);
        $('input[name=gender]').on('change', enter_details_validate);

        function enter_details_validate() {
            var age = $('#age').val(),
                gender = $('input[name=gender]:checked').val();

            if (!age) {
                if(gender)
                    details_continue.tooltip('option', 'content', 'Enter your age to continue.')
                        .tooltip('option', 'disabled', false);
                else
                    details_continue.tooltip('option', 'content', 'Enter your age and specify your gender to continue.')
                        .tooltip('option', 'disabled', false);
            }
            else {
                if(gender)
                    details_continue.tooltip('option', 'disabled', true);
                else
                    details_continue.tooltip('option', 'content', 'Specify your gender to continue.')
                        .tooltip('option', 'disabled', false);
            }
        }

        // Show about dialogue
        $('.about-button').click(function () {
            $('.about-dialogue').fadeIn();
        });

        // Hide about dialogue
        $('.close').click(function () {
            $('.about-dialogue').fadeOut();
        });

        // End conversation dialogue
        $('#end').click(function () {
                yes_no_dialog.dialog('option', 'title', 'End conversation')
                    .text("Are you sure you want to end this conversation early?")
                    .dialog('option', 'buttons', [
                        {
                            text: "Yes",
                            click: function () {
                                $(this).dialog('close');
                                endConvo()
                            }
                        },
                        {
                            text: "No",
                            click: function () {
                                $(this).dialog('close');
                            }
                        }
                    ])
                    .dialog('open');
            }
        );
    });

    // --- SERVER LISTENERS ---
    
    /* 
     * Matched on the server side 
     *
     * @data contains match's username and a room ID (not sure if we need it on the client side,
     * maybe it can be used on a better message origin/acknowledgement check 
     */ 
    socket.on("matchfound", function (data) {
        match_username = data.matchUsername;
        var roomID = data.roomID;

        // Debugging stuff
        // console.log("Matched. Room ID: " + console.log(roomID) + ", match username: " +console.log(match_username));

        // Stop waiting animation/text;
        $('.enter-details').slideUp();

        // Display match's username
        $('#chat-info').text(match_username);

        //Display chat
        showChat();

        // TODO start timer?
    });

    /* Sending and Receiving Messages */
    socket.on("message", function (data) {
        if (data.username == my_username) {
            // Server sent the message back to us, so we know it was sent
            // Remove pending class so message is confirmed as sent to the user
            setTimeout(function () {
                $(".pending").removeClass("pending");
            }, 100);

        }
        else {
            receiveMessage(data);
        }
    });

    /*
     * Toggle the typing indicator
     */
    // Default fade animation time seemed to be to much for very fast typing and lagged behind,
    // but now doesn't look like an animation anymore. TODO play with fadeAnimationTime to find a compromise
    var fadeAnimationTime = 50;
    socket.on("isTyping", function (data) {
        console.log("other user is typing");
        if (data == true) {
            $('.typing-indicator').fadeIn(fadeAnimationTime)
        }
        else {
            $('.typing-indicator').fadeOut(fadeAnimationTime);
        }
    });
    
    // Message received is from other user
    function receiveMessage(data) {
        // Get username and message
        var match_username = data.username,
            match_message = data.message;

        // Add message to the chat window
        var received_message =
            "<li class='message'>" +
            "<span class='message-text match'>" +
            match_message +
            "</span> </li>";

        $('.message-list').append(received_message);
        autoScroll();
    }

    // Send a message to the server
    function sendMessage() {

        var type_message = $('#type-message');

        // Reset typing
        clearTimeout(timeout);
        timeout = setTimeout(typingTimeout, 0);

        if (type_message.val().length > 0) {
            // Add message to the chat window
            // Pending class used to grey out message until we know the server got it.
            var client_message =
                "<li class='message'>" +
                "<span class='message-text me pending'>" +
                type_message.val() +
                "</span> </li>";
            $('.message-list').append(client_message);

            // Sends raw message to server
            socket.send({username: my_username, message: type_message.val()});

            // Clear text entry
            type_message.val("");
            autoScroll();

            // Reset placeholder message
            type_message.attr('placeholder', 'Press Enter to send');
        }
        else {
            type_message.attr('placeholder', 'Please type a message to send it').val("");
        }
    }

    /* TYPING */

    // When user stops typing, reset the variables and tell the server
    function typingTimeout() {
        typing = false;
        socket.emit("isTyping", false);
    }

    /* MISC FUNCTIONS */
    function endConvo() {
        // Leave the room
        socket.emit("leaveroom", "test");

        $('.chat-section').slideUp();
        $('#warning-dialog').dialog('option', 'title', "It's over!")
            .text("Time's up, the conversation is now finished.")
            .dialog('option', 'buttons', [
                {
                    text: "Okay",
                    click: function () {
                        location.reload()
                    }
                }
            ])
            .dialog('open');
    }

    function autoScroll() {
        var chat_window = $('.chat-window').get(0);
        chat_window.scrollTop = chat_window.scrollHeight;
    }

    function showWarning(title, message) {
        $('#warning-dialog').dialog('option', 'title', title)
            .text(message)
            .dialog('open')
    }

    /* Main stuff happens below!!! */

    // Load external views
    var view_enter_name = $.get('views/enter-name.html', function (enter_name) {
        $('.content').append(enter_name);
    });

    var view_enter_details = $.get('views/enter-details.html', function (enter_details) {
        $('.content').append(enter_details);
    });

    var view_chat_section = $.get('views/chat-section.html', function (chat_section) {
        $('.content').append(chat_section);
    });

    var about_float = $.get('views/about-float.html', function (about_float) {
        $('.content').append(about_float);
    });


    $.when(view_enter_name, view_enter_details, view_chat_section, about_float).done(function () {
        
        // Show name field
        $('.enter-name').slideDown();

        // When name entered
        $('#name-continue').click(function (event) {
            event.preventDefault();
            my_username = $('#name').val();
            if (my_username.length > 0) {
                $('#name-continue').prop('disabled', true).html('Loading...');
                setTimeout(function () {
                    $('.enter-name').slideUp();
                }, 500); //simulate connecting with back-end
                setTimeout(showDetails, 1000);
            }
        });

        // Show details entering view
        function showDetails() {
            $('.enter-details').slideDown();

            // Brings focus to age text box as soon as page loads
            $("#age").focus();

            $('#details-continue').click(function (event) {

                event.preventDefault();

                // Get entered age
                my_age = $('#age').val();

                // Get gender chosen
                my_gender = $('input[name=gender]:checked').val();

                if (my_age && my_gender) {
                    $('#details-continue').prop('disabled', true).html('Matching...');
                    // TODO add a timeout, but in our given scenario everyone will be eventualy matched
                    // maybe implement a request acknowledgement on the server side?
                    getMatch();
                }
            })
        }
        
        // Request a match from the server
        function getMatch() {
            socket.emit("register", {name: my_username, age: my_age, gender: my_gender});
            // TODO maybe here is a good idea to display some waiting animation until server replays with a match
            
        }

        // Gives the user options in the event a match can't be found
        function retryMatch() {
            
            // If no match is found
            $('#yes-no-dialog').dialog('option', 'title', 'No Match!')
                    .text("No match was found! Would you like to retry or return to the beginning?")
                    .dialog('option', 'buttons', [
                        {
                            text: "Retry",
                            click: function () {
                                $(this).dialog('close');
                                getMatch();
                            }
                        },
                        {
                            text: "Reset",
                            click: function () {
                                $(this).dialog('close');
                                location.reload()
                            }
                        }
                    ])
                    .dialog('open');
        }
    })
        // Show chat view
        function showChat() {
            $('.chat-section').slideDown();

            // Bring focus to text-entry box
            $("#type-message").focus();

            // Start timer
            /*
             var start = new Date;
             setInterval(function () {

             // Total length of time you want the conversation to be
             var totalTime = 180;

             var time = totalTime - Math.floor((new Date - start) / 1000);
             var countdown = function () {
             if (time > 0) {
             return time;
             }
             else if (time == 0) {
             endConvo(); //ends conversation when the timer reaches 0
             }
             };
             $('#timer').text(countdown() + " seconds");
             }, 1000);
             */
        }
})(jQuery);
