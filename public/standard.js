function deleteActivity(obj) {
  var activity = $(obj.target).closest("article");
  var id = activity.attr("id");

  $.ajax({url: '/posts/' + id,
          type: 'DELETE',
          success: function (data) {
            console.log (data);
            activity.hide();
          },
          error: function (request, status) {
            console.log(request);
          }});
  return false;
}

$(function () {
  $(".delete-activity").live("click", deleteActivity);

  var ws = $.websocket("ws://beta.jiksnu.com/main/events", {
    events: {
      message: function(e) {
        $('#content').append(e.data + '<br>')}}});
})
