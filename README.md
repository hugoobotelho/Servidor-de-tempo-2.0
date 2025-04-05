# Servidor de Tempo

## Descrição
Este é um servidor de tempo, onde multiplas máquinas comunicam-se para demonstrar o funcionamento real do serviço. Uma máquina atua como o cliente, solicitando a requisição de tempo, enquanto a outra serve como o servidor, oferecendo o serviço de tempo. Nessa nova atualização é possível mais de um servidor rodar ao mesmo tempo, aumentando a disponibilidade do serviço e encaminhando para uma aplicação distribuida
## Tecnologia Utilizada
- Java

## Como funciona
1. O servidor deve ser executado em uma ou mais máquinas conectadas a uma rede.
2. O cliente deve ser executado em outra máquina na mesma rede que o servidor.
3. No socket UDP o cliente não precisa inserir nenhum ip pois ele descobre o ip dos servidores via broadcast. No entanto, é necessário colocar pelo menos o ip de um servidor no TCP.
4. Quando o cliente clica no botão de "atualizar", ele envia uma requisição ao servidor, utilizando o protocolo TCP ou UDP, conforme a escolha do tipo de socket pelo usuário. O servidor, por sua vez, responde com a hora atual.

## Como Executar o Projeto
```bash
# Clone o repositório
git clone https://github.com/hugoobotelho/Servidor-de-tempo.git

# Acesse a pasta do projeto
cd Servidor-de-tempo

# Compile e rode o servidor em uma maquina
cd socketTCP
cd servidor
javac Principal.java
java Principal

# Compile e rode o cliente em outra maquina
cd socketTCP
cd cliente
javac Principal.java
java Principal
```

## Funcionamento
![Funcionamento](https://github.com/hugoobotelho/Servidor-de-tempo/raw/main/funcionamento.gif)

## Contribuição
Se quiser contribuir com melhorias, fique à vontade para abrir um Pull Request.

## Licença
Este projeto está sob a licença MIT.

