
window.addEventListener("load", function ()
{
	console.log("Page loaded");
});

/*
 * Inyectamos dependencias para las animaciones (ngAnimate) y para el scroll
 * glue (scroll automático con luegg.directives).
 */
angular
	.module("ChatApp", ["ngAnimate", "luegg.directives"])
	.controller("MessageController", MessageController);

function MessageController($scope, $http)
{
	$scope.connected = false;
	$scope.newMessage = {};
	$scope.messages = [];


	var host = location.hostname == "localhost" ?
		"localhost:9000" : location.hostname;
	var socket;


	function checkSuccess(data)
	{
		$scope.unique = data.unique;
	}

	function checkError(data)
	{
		console.log(data);
	}

	$scope.checkName = function ()
	{
		$http.get("http://" + host + "/check/" + $scope.newMessage.author)
				.success(checkSuccess).error(checkError);
	};

	$scope.setName = function ()
	{
		socket = new WebSocket("ws://" + host + "/chat/" + $scope.newMessage.author);

		socket.onopen = function ()
		{
			console.log("Connected");
			$scope.connected = true;

			$scope.$apply();
		};

		socket.onclose = function ()
		{
			console.log("Disconnected");
			$scope.connected = false;

			$scope.$apply();
		};

		socket.onmessage = function (e)
		{
			var msg = JSON.parse(e.data);
			$scope.messages.push(msg);

			$scope.$apply();
		};

		// Para evitar desconexiones enviamos mensajes vacíos cada 30 segundos.
		setInterval(function ()
		{
			// Creamos el objeto a enviar
			var obj = {};
			obj.msgText = "";

			// Enviamos el mensaje al servidor.
			socket.send(JSON.stringify(obj));
		}, 30000);
	};

	/*
	 * Función llamada el pulsar sobre el botón "Enviar".
	 */
	$scope.sendMessage = function ()
	{
		// Creamos el objeto a enviar
		var obj = {};
		obj.msgText = $scope.newMessage.msgText;

		// No enviamos mensajes vacíos.
		if (obj.msgText == "")
		{
			return;
		}

		// Enviamos el mensaje al servidor.
		socket.send(JSON.stringify(obj));

		// Añadimos el mensaje a los existentes en la conversación.
		obj.author = "Yo";
		$scope.messages.push(obj);

		// Vaciamos el input text para que el usuario escriba un nuevo msg.
		$scope.newMessage.msgText = "";
	};
}
