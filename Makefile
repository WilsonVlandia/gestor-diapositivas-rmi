.PHONY: servidor cliente

# Arranca el presentador (compila y ejecuta servidor-app/).
servidor:
	@bash servidor-app/Iniciar-Servidor.command

# Arranca el control remoto (compila y ejecuta cliente-app/).
# Al iniciar pide la IP del servidor y solicita la conexion automaticamente.
cliente:
	@bash cliente-app/Iniciar-Cliente.command
