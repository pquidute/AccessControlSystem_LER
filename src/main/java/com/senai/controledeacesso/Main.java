package com.senai.controledeacesso;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {
    private static final File pastaControleDeAcesso = new File(System.getProperty("user.home"), "ControleDeAcesso");
    private static final File databaseStudent = new File(pastaControleDeAcesso, "databaseStudent.txt");
    private static final File databaseADM = new File(pastaControleDeAcesso, "databaseADM.txt");
    private static final File databaseAQV = new File(pastaControleDeAcesso, "databaseAQV.txt");
    private static final File arquivoRegistrosDeAcesso = new File(pastaControleDeAcesso, "registrosDeAcesso.txt");
    public static final File pastaImagens = new File(pastaControleDeAcesso, "imagens");
    public static ArrayDeque<Object> arrayListImagens;

    //Arrays
    static ArrayList<Student> arrayStudents = new ArrayList<>();
    static ArrayList<AQV> arrayAQV = new ArrayList<>();
    static ArrayList<ADM> arrayADM = new ArrayList<>();

    //MQTT server properties
    static volatile boolean modoCadastrarIdAcesso = false;
    static int idUsuarioRecebidoPorHTTP = 0;
    static String dispositivoRecebidoPorHTTP = "Disp1";
    static String brokerUrl = "tcp://localhost:1883";  // Exemplo de
    static String topico = "IoTKIT1/UID";
    static CLienteMQTT conexaoMQTT;
    static ServidorHTTPS servidorHTTPS;
    static Scanner scanner = new Scanner(System.in);
    static ExecutorService executorIdentificarAcessos = Executors.newFixedThreadPool(4);
    static ExecutorService executorCadastroIdAcesso = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        verificarEstruturaDeDiretorios();
        carregarDadosDoArquivo();
        conexaoMQTT = new CLienteMQTT(brokerUrl, topico, Main::processarMensagemMQTTRecebida);
        servidorHTTPS = new ServidorHTTPS(); // Inicia o servidor HTTPS

        //TEST
        arrayADM.add(new ADM(new User("Pedro", "pedro@icloud.com", "123456")));

        paginaDeLogin();

        // Finaliza o todos os processos abertos ao sair do programa
        scanner.close();
        executorIdentificarAcessos.shutdown();
        executorCadastroIdAcesso.shutdown();
        conexaoMQTT.desconectar();
        servidorHTTPS.pararServidorHTTPS();
    }

    //Login
        private static void paginaDeLogin() {
        System.out.println("BEM VINDO!\nFaça seu login para continuar:\n1 - Login para Coordenador\n2 - Login para AQV\n3 - Login para Aluno");
        int menu = scanner.nextInt();
        scanner.nextLine();
        switch (menu) {
            case 1:
                ADM adm = loginADM();
                if (adm != null) {
                    menuADM(adm);
                } else break;
            case 2:
                AQV aqv = loginAQV();
                if (aqv != null) {
                    menuAQV(aqv);
                }
                break;
            case 3:
                Student student = loginStudent();
                if (student != null) {
                    menuStudent(student);
                }
                break;
        }
    }
        private static ADM loginADM() {
            System.out.print("Email: ");
            String identifier = scanner.nextLine();
            System.out.print("Senha: ");
            String password = scanner.nextLine();
            boolean userFound = false; // Track if user is found
            for (int i = 0; i < arrayADM.size(); i++) {
                ADM adm = arrayADM.get(i); // Get the object at index i
                if (adm.user != null && adm.user.identifier.equals(identifier)) { // Compare email
                    userFound = true; // Mark user as found
                    boolean login = adm.user.Login(identifier, password); // Call the login method
                    if (login) {
                        System.out.println("Login bem sucedido!");
                        return adm;
                    } else {
                        System.out.println("Senha incorreta, tente novamente!");
                    }
                }
            }
            if (!userFound) {
                System.out.println("Usuário não encontrado, tente novamente");
            }
            return null;
        }
        private static Student loginStudent() {
            System.out.print("Número de Matricula: ");
            String identifier = scanner.nextLine();
            System.out.print("Senha: ");
            String password = scanner.nextLine();
            boolean userFound = false; // Track if user is found
            for (int i = 0; i < arrayStudents.size(); i++) {
                Student student = arrayStudents.get(i); // Get the object at index i
                if (student.user != null && student.user.identifier.equals(identifier)) { // Compare email
                    userFound = true; // Mark user as found
                    boolean login = student.user.Login(identifier, password); // Call the login method
                    if (login) {
                        System.out.println("Login bem sucedido!");
                        return student;
                    } else {
                        System.out.println("Senha incorreta, tente novamente!");
                    }
                }
            }
            if (!userFound) {
                System.out.println("Usuário não encontrado, tente novamente");
            }
            return null;
        }
        private static AQV loginAQV() {
            System.out.print("Email: ");
            String identifier = scanner.nextLine();
            System.out.print("Senha: ");
            String password = scanner.nextLine();
            boolean userFound = false; // Track if user is found
            for (int i = 0; i < arrayADM.size(); i++) {
                AQV aqv = arrayAQV.get(i); // Get the object at index i
                if (aqv.user != null && aqv.user.identifier.equals(identifier)) { // Compare email
                    userFound = true; // Mark user as found
                    boolean login = aqv.user.Login(identifier, password); // Call the login method
                    if (login) {
                        System.out.println("Login bem sucedido!");
                        return aqv;
                    } else {
                        System.out.println("Senha incorreta, tente novamente!");
                    }
                }
            }
            if (!userFound) {
                System.out.println("Usuário não encontrado, tente novamente");
            }
            return null;
        }

        //Menus
        private static void menuADM(ADM adm) {
        int opcao;
        do {
            String menu = """
                    _________________________________________________________
                    |   Escolha uma opção:                                  |
                    |       1- Exibir cadastro completo                     |
                    |       2- Inserir novo cadastro                        |
                    |       3- Atualizar cadastro por id                    |
                    |       4- Deletar um cadastro por id                   |
                    |       5- Associar TAG ou cartão de acesso ao usuário  |
                    |       6- Sair                                         |
                    _________________________________________________________
                    """;
            System.out.println(menu);
            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    exibirCadastro();
                    break;
                case 2:
                    cadastrarUsuario(1);
                    break;
                case 3:
                    atualizarUsuario();
                    break;
                case 4:
                    deletarUsuario();
                    break;
                case 5:
                    aguardarCadastroDeIdAcesso();
                    break;
                case 6:
                    System.out.println("Fim do programa!");
                    break;
                default:
                    System.out.println("Opção inválida!");
                }

            } while (opcao != 6);
        }
        private static void menuAQV(AQV aqv) {
        int opcao;
        do {
            String menu = """
                    _________________________________________________________
                    |   Escolha uma opção:                                  |
                    |       1- Exibir cadastro completo                     |
                    |       2- Inserir novo cadastro                        |
                    |       3- Atualizar cadastro por id                    |
                    |       4- Deletar um cadastro por id                   |
                    |       5- Associar TAG ou cartão de acesso ao usuário  |
                    |       6- Sair                                         |
                    _________________________________________________________
                    """;
            System.out.println(menu);
            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    exibirCadastro();
                    break;
                case 2:
                    cadastrarUsuario(2);
                    break;
                case 3:
                    atualizarUsuario();
                    break;
                case 4:
                    deletarUsuario();
                    break;
                case 5:
                    aguardarCadastroDeIdAcesso();
                    break;
                case 6:
                    System.out.println("Fim do programa!");
                    break;
                default:
                    System.out.println("Opção inválida!");
            }

        } while (opcao != 6);
    }
        private static void menuStudent(Student student) {
        System.out.println("\nBem vindo " + student.user.name + "!\n1. Consultar meus dados\n2. Consultar meus atrasos\n3. Atualizar meus dados");
        int menu3 = scanner.nextInt();
        switch (menu3) {
            case 1:
                System.out.println(student);
                break;
            case 2:
                student.showAccessRegisters();
                break;
            case 3:
                //data update function
                break;
            default:
                System.out.println("Opção inválida!");
        }
    }


        private static void aguardarCadastroDeIdAcesso () {
            modoCadastrarIdAcesso = true;
            System.out.println("Aguardando nova tag ou cartão para associar ao usuário");
            // Usar Future para aguardar até que o cadastro de ID seja concluído
            Future<?> future = executorCadastroIdAcesso.submit(() -> {
                while (modoCadastrarIdAcesso) {
                    // Loop em execução enquanto o modoCadastrarIdAcesso estiver ativo
                    try {
                        Thread.sleep(100); // Evita uso excessivo de CPU
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            try {
                future.get(); // Espera até que o cadastro termine
            } catch (Exception e) {
                System.err.println("Erro ao aguardar cadastro: " + e.getMessage());
            }
        }
        private static void processarMensagemMQTTRecebida (String mensagem){
            if (!modoCadastrarIdAcesso) {
                executorIdentificarAcessos.submit(() -> criarNovoRegistroDeAcesso(mensagem)); // Processa em thread separada
            } else {
                cadastrarNovoIdAcesso(mensagem); // Processa em thread separada
                modoCadastrarIdAcesso = false;
                idUsuarioRecebidoPorHTTP = 0;
            }
        }

        // Função que busca e atualiza a tabela com o ID recebido
        private static void criarNovoRegistroDeAcesso (String idAcessoRecebido){
            boolean usuarioEncontrado = false; // Variável para verificar se o usuário foi encontrado
            int idAcessoInt;
            try {
                idAcessoInt = Integer.parseInt(idAcessoRecebido);  // Convert String 'idAcessoRecebido' to int
            } catch (NumberFormatException e) {
                System.out.println("O ID fornecido não é um número válido.");
                return; // Exit the method if conversion fails
            }
            int currentStudentIndex = 0;
            for (int i = 0; i < arrayStudents.size(); i++) {
                Student currentStudent = arrayStudents.get(i); // Get the current student
                if (currentStudent.user.ID == idAcessoInt) { // Match by ID
                    // Increment the integer attribute in the Student class
                    currentStudent.delays += 1;
                    currentStudentIndex = arrayAQV.indexOf(currentStudent);
                }
            }
                if (arrayStudents.get(currentStudentIndex).user.ID == idAcessoInt) {
                    Student currentStudent = arrayStudents.get(currentStudentIndex);
                    currentStudent.arrayDelays.add(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    usuarioEncontrado = true; // Marca que o usuário foi encontrado
                    return;
                }
            // Se não encontrou o usuário, imprime uma mensagem
            if (!usuarioEncontrado) {
                System.out.println("Id de Acesso " + idAcessoRecebido + " não cadastrado.");
            }
        }

        private static void cadastrarNovoIdAcesso (String novoIdAcesso) {
            boolean encontrado = false; // Variável para verificar se o usuário foi encontrado
            int idUsuarioEscolhido = idUsuarioRecebidoPorHTTP;
            String dispositivoEscolhido = dispositivoRecebidoPorHTTP;

            if (idUsuarioRecebidoPorHTTP == 0) {
                // Exibe a lista de usuários para o administrador escolher
                for (int i = 0; i < arrayStudents.size(); i++) {
                    System.out.println(arrayStudents.get(i).toString());
                }
                // Pede ao administrador que escolha o ID do usuário
                System.out.print("Digite o ID do usuário para associar ao novo idAcesso: ");
                idUsuarioEscolhido = scanner.nextInt();
                conexaoMQTT.publicarMensagem(topico, dispositivoEscolhido);
            }
            modoCadastrarIdAcesso = true;
            // Verifica se o ID do usuário existe na matriz
            for (int i = 0; i < arrayStudents.size(); i++) {
                if (arrayStudents.get(i).user.ID == idUsuarioEscolhido){
                    arrayStudents.get(i).accessId = Integer.parseInt(novoIdAcesso);
                    System.out.println("ID de acesso " + novoIdAcesso + " associado ao usuário " + arrayStudents.get(i).user.name);
                    conexaoMQTT.publicarMensagem("cadastro/disp", "CadastroConcluido");
                    encontrado = true;
                    salvarDados();
                    break;
                }
            }
            // Se não encontrou o usuário, imprime uma mensagem
            if (!encontrado) {
                System.out.println("Usuário não encontrado!");
            }
        }

        // Funções de CRUD
        private static void exibirCadastro () {
            for (int i = 0; i < arrayStudents.size(); i++) {
                System.out.println(arrayStudents.get(i).toString());
            }
            if (arrayStudents == null){
                System.out.println("Não há usuários no sistema");
            }
        }

        private static void cadastrarUsuario (int tipoDeUsuario) {

            switch (tipoDeUsuario){
                case 1:
                    System.out.println("Qual será o tipo de usuário?\n1. ADM(coordenador)\n2. AQV\n3. Aluno");
                    int menu = scanner.nextInt();
                    switch (menu){
                        case 1:
                            //create ADM
                            break;
                        case 2:
                            //create AQV
                            break;
                        case 3:
                            System.out.println("Quantos alunos serão cadastrados?");
                            int qtdUsuarios = scanner.nextInt();
                            scanner.nextLine();
                            for (int i = 0; i < qtdUsuarios; i++) {
                                System.out.print("ID: ");
                                String ID = scanner.nextLine();
                                System.out.print("NOME: ");
                                String name = scanner.nextLine();
                                System.out.print("Número de Matrícula: ");
                                String identifier = scanner.nextLine();
                                System.out.print("Senha: ");
                                String password = scanner.nextLine();
                                System.out.print("TURMA: ");
                                String classroom = scanner.nextLine();
                                arrayStudents.add(new Student(new User(name, identifier, password), classroom));
                                for (int j = 0; j < arrayStudents.size(); j++) {
                                    if (arrayStudents.get(i).user.identifier.equals(identifier)){
                                        arrayStudents.get(i).user.ID = Integer.parseInt(ID);
                                    }
                                }
                            }
                            break;
                    }
                    break;
                case 2:
                    System.out.println("Quantos alunos serão cadastrados?");
                    int qtdUsuarios = scanner.nextInt();
                    scanner.nextLine();
                    for (int i = 0; i < qtdUsuarios; i++) {
                        System.out.print("ID: ");
                        String ID = scanner.nextLine();
                        System.out.print("NOME: ");
                        String name = scanner.nextLine();
                        System.out.print("Número de Matrícula: ");
                        String identifier = scanner.nextLine();
                        System.out.print("Senha: ");
                        String password = scanner.nextLine();
                        System.out.print("TURMA: ");
                        String classroom = scanner.nextLine();
                        arrayStudents.add(new Student(new User(name, identifier, password), classroom));
                    }
                    break;
            }
            salvarDados();
        }
        private static void atualizarUsuario () {
            exibirCadastro();
            System.out.println("Escolha um id para atualizar o cadastro:");
            int idUsuario = scanner.nextInt();
            scanner.nextLine();
            scanner.nextLine();
            System.out.println("\nAtualize os dados a seguir:");
            for (int i = 0; i < arrayStudents.size(); i++) {
                if (arrayStudents.get(i).user.ID == idUsuario){
                    System.out.println("Qual dado será atualizado?\n1. Todos\n2. Nome\n3. Número de Matrícula\n4. Senha\n5. ID\n6; Turma\n7. Quantidade de atrasos");
                    int menu = scanner.nextInt();
                    switch (menu){
                        case 1:
                            System.out.print("\n--------------------ATUALIZAÇÃO DE DADOS--------------------\nNome: ");
                            String newName = scanner.nextLine();
                            System.out.print("Número de Matrícula: ");
                            String newIdentifier  = scanner.nextLine();
                            System.out.print("Senha: ");
                            String newPassword = scanner.nextLine();
                            System.out.print("ID: ");
                            int newID = scanner.nextInt();
                            scanner.nextLine();
                            System.out.print("Turma: ");
                            String newClassroom = scanner.nextLine();
                            System.out.print("Quantidade de atrasos: ");
                            int delays = scanner.nextInt();
                            scanner.nextLine();
                            arrayStudents.get(i).user.name = (newName);
                            arrayStudents.get(i).user.identifier = (newIdentifier);
                            arrayStudents.get(i).user.password = (newPassword);
                            arrayStudents.get(i).user.ID = (newID);
                            arrayStudents.get(i).classroom = newClassroom;
                            arrayStudents.get(i).delays = delays;
                            System.out.println("Dados atualizados com sucesso!");
                            System.out.println(arrayStudents.get(i).toString());
                            break;
                        case 2:
                            System.out.print("\n--------------------ATUALIZAÇÃO DE NOME--------------------\nNovo nome: ");
                            newName = scanner.nextLine();
                            arrayStudents.get(i).user.name = (newName);
                            System.out.println("Nome atualizado com sucesso!");
                            System.out.println(arrayStudents.get(i).toString());
                            break;
                        case 3:
                            System.out.print("\n--------------------ATUALIZAÇÃO DE IDENTIFICADOR--------------------\nNovo número de matrícula: ");
                            newIdentifier = scanner.nextLine();
                            arrayStudents.get(i).user.identifier = (newIdentifier);
                            System.out.println("Número de matrícula atualizado com sucesso!");
                            System.out.println(arrayStudents.get(i).toString());
                            break;
                        case 4:
                            System.out.print("\n--------------------ATUALIZAÇÃO DE SENHA--------------------\nNova senha: ");
                            newPassword = scanner.nextLine();
                            arrayStudents.get(i).user.password = (newPassword);
                            System.out.println("Senha atualizada com sucesso!");
                            System.out.println(arrayStudents.get(i).toString());
                            break;
                        case 5:
                            System.out.print("\n--------------------ATUALIZAÇÃO DE ID--------------------\nNovo ID: ");
                            newID = scanner.nextInt();
                            arrayStudents.get(i).user.ID = (newID);
                            System.out.println("ID atualizado com sucesso!");
                            System.out.println(arrayStudents.get(i).toString());
                            break;
                        case 6:
                            System.out.print("\n--------------------ATUALIZAÇÃO DE TURMA--------------------\nNova Turma: ");
                            newClassroom = scanner.nextLine();
                            arrayStudents.get(i).classroom = (newClassroom);
                            System.out.println("Turma atualizada com sucesso!");
                            System.out.println(arrayStudents.get(i).toString());
                            break;
                        case 7:
                            System.out.print("\n--------------------ATUALIZAÇÃO DE ATRASOS--------------------\nQuantidade de atrasos: ");
                            delays = scanner.nextInt();
                            arrayStudents.get(i).delays = delays;
                            System.out.println("Atrasos atualizados com sucesso!");
                            System.out.println(arrayStudents.get(i).toString());
                            break;
                        default:
                            System.out.println("Opção inválida");
                    }
                }
            }
            salvarDados();
        }
        public static void deletarUsuario () {
            exibirCadastro();
            System.out.println("Escolha um id para deletar o cadastro:");
            int idUsuario = scanner.nextInt();
            scanner.nextLine();
            for (int i = 0; i < arrayStudents.size(); i++) {
                if(arrayStudents.get(i).user.ID == idUsuario){
                    arrayStudents.remove(i);
                    break;
                }
                else System.out.println("ID não encontrado!");
            }
            salvarDados();
            System.out.println("-----------------------Usuário deletado com sucesso------------------------\n");
            idUsuarioRecebidoPorHTTP = 0;
        }

        // Funções para persistência de dados
        public static void carregarDadosDoArquivo() {
            try {
                // Read data for students
                try (BufferedReader reader = new BufferedReader(new FileReader(databaseStudent))) {
                    String line;
                    // Skip the header row
                    reader.readLine();

                    while ((line = reader.readLine()) != null) {
                        String[] data = line.split(",");

                        // Ensure that there are enough fields
                        if (data.length >= 8) {
                            String name = data[1];
                            String identifier = data[2];
                            String password = data[3];
                            String classroom = data[4];
                            String delays = data[5];
                            String accessId = data[6];

                            // Parse arrayDelays from the semicolon-separated string
                            ArrayList<String> arrayDelays = new ArrayList<>();
                            String[] delaysArray = data[7].split(";");
                            for (String delay : delaysArray) {
                                arrayDelays.add(delay);
                            }

                            // Create a User object
                            User user = new User(name, identifier, password); // No ID field here

                            // Create a Student object and set the arrayDelays
                            Student student = new Student(user, classroom);
                            student.delays = Integer.parseInt(delays);  // Set the delays count (optional)
                            student.accessId = Integer.parseInt(accessId);  // Set the accessId (optional)
                            student.arrayDelays = arrayDelays;  // Set the arrayDelays

                            arrayStudents.add(student);
                        } else {
                            System.err.println("Erro: Linha com número insuficiente de campos: " + line);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Erro ao ler os dados dos estudantes: " + e.getMessage(), e);
                }

                // Read data for ADM
                try (BufferedReader reader = new BufferedReader(new FileReader(databaseADM))) {
                    String line;
                    // Skip the header row
                    reader.readLine();

                    while ((line = reader.readLine()) != null) {
                        String[] data = line.split(",");

                        // Ensure the line has enough fields
                        if (data.length >= 4) {
                            String name = data[1];
                            String identifier = data[2];
                            String password = data[3];

                            // Create and add the ADM object
                            User user = new User(name, identifier, password); // No ID field here
                            ADM adm = new ADM(user);
                            arrayADM.add(adm);
                        } else {
                            System.err.println("Erro: Linha com número insuficiente de campos para ADM: " + line);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Erro ao ler os dados dos administradores: " + e.getMessage(), e);
                }

                // Read data for AQV
                try (BufferedReader reader = new BufferedReader(new FileReader(databaseAQV))) {
                    String line;
                    // Skip the header row
                    reader.readLine();

                    while ((line = reader.readLine()) != null) {
                        String[] data = line.split(",");

                        // Ensure the line has enough fields
                        if (data.length >= 4) {
                            String name = data[1];
                            String identifier = data[2];
                            String password = data[3];

                            // Create and add the AQV object
                            User user = new User(name, identifier, password); // No ID field here
                            AQV aqv = new AQV(user);
                            arrayAQV.add(aqv);
                        } else {
                            System.err.println("Erro: Linha com número insuficiente de campos para AQV: " + line);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Erro ao ler os dados de AQV: " + e.getMessage(), e);
                }
            } catch (Exception e) {
                System.err.println("Erro ao carregar os dados: " + e.getMessage());
            }
        }
        public static void salvarDados() {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(databaseStudent))) {
                // Write the header row
                writer.write("ID,Name,Identifier,Password,Classroom,Delays,AccessId,ArrayDelays");
                writer.newLine();
                // Write data for each student in the list
                for (Student student : arrayStudents) {
                    StringBuilder linha = new StringBuilder();
                    // Add User data
                    linha.append(student.user.ID).append(",");
                    linha.append(student.user.name).append(",");
                    linha.append(student.user.identifier).append(",");
                    linha.append(student.user.password).append(",");
                    // Add Student-specific data
                    linha.append(student.classroom).append(",");
                    linha.append(student.delays).append(",");
                    linha.append(student.accessId).append(",");
                    // Add arrayDelays as a single concatenated string
                    linha.append(String.join(";", student.arrayDelays));
                    // Write the line
                    writer.write(linha.toString());
                    writer.newLine();
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(databaseADM))) {
                // Write the header row
                writer.write("ID,Name,Identifier,Password");
                writer.newLine();
                // Write data for each ADM in the list
                for (ADM adm : arrayADM) {
                    StringBuilder linha = new StringBuilder();
                    // Add User data from the ADM object
                    linha.append(adm.user.ID).append(",");
                    linha.append(adm.user.name).append(",");
                    linha.append(adm.user.identifier).append(",");
                    linha.append(adm.user.password);
                    // Write the line
                    writer.write(linha.toString());
                    writer.newLine();
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(databaseAQV))) {
                // Write the header row
                writer.write("ID,Name,Identifier,Password");
                writer.newLine();
                // Write data for each ADM in the list
                for (AQV aqv : arrayAQV) {
                    StringBuilder linha = new StringBuilder();
                    // Add User data from the ADM object
                    linha.append(aqv.user.ID).append(",");
                    linha.append(aqv.user.name).append(",");
                    linha.append(aqv.user.identifier).append(",");
                    linha.append(aqv.user.password);
                    // Write the line
                    writer.write(linha.toString());
                    writer.newLine();
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        private static void verificarEstruturaDeDiretorios () {
            // Verifica se a pasta ControleDeAcesso existe, caso contrário, cria
            if (!pastaControleDeAcesso.exists()) {
                if (pastaControleDeAcesso.mkdir()) {
                    System.out.println("Pasta ControleDeAcesso criada com sucesso.");
                } else {
                    System.out.println("Falha ao criar a pasta ControleDeAcesso.");
                }
            }

            // Verifica se o arquivo databaseAQV.txt existe, caso contrário, cria
            if (!databaseAQV.exists()) {
                try {
                    if (databaseAQV.createNewFile()) {
                        System.out.println("Arquivo databaseAQV.txt criado com sucesso.");
                    } else {
                        System.out.println("Falha ao criar o arquivo databaseAQV.txt.");
                    }
                } catch (IOException e) {
                    System.out.println("Erro ao criar arquivo databaseAQV.txt: " + e.getMessage());
                }
            }

            // Verifica se o arquivo databaseADM.txt existe, caso contrário, cria
            if (!databaseADM.exists()) {
                try {
                    if (databaseADM.createNewFile()) {
                        System.out.println("Arquivo databaseADM.txt criado com sucesso.");
                    } else {
                        System.out.println("Falha ao criar o arquivo databaseADM.txt.");
                    }
                } catch (IOException e) {
                    System.out.println("Erro ao criar arquivo databaseADM.txt: " + e.getMessage());
                }
            }

            // Verifica se o arquivo databaseStudent.txt existe, caso contrário, cria
            if (!databaseStudent.exists()) {
                try {
                    if (databaseStudent.createNewFile()) {
                        System.out.println("Arquivo databaseStudent.txt criado com sucesso.");
                    } else {
                        System.out.println("Falha ao criar o arquivo databaseStudent.txt.");
                    }
                } catch (IOException e) {
                    System.out.println("Erro ao criar arquivo databaseStudent.txt: " + e.getMessage());
                }
            }

            // Verifica se a pasta imagens existe, caso contrário, cria
            if (!pastaImagens.exists()) {
                if (pastaImagens.mkdir()) {
                    System.out.println("Pasta imagens criada com sucesso.");
                } else {
                    System.out.println("Falha ao criar a pasta imagens.");
                }
            }
        }
}
