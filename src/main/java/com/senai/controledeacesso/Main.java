package com.senai.controledeacesso;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {
    private static final File pastaControleDeAcesso = new File(System.getProperty("user.home"), "ControleDeAcesso");
    private static final File arquivoBancoDeDados = new File(pastaControleDeAcesso, "bancoDeDados.txt");
    public static final File pastaImagens = new File(pastaControleDeAcesso, "imagens");

    //Arrays
    static ArrayList<Student> arrayStudents = new ArrayList<>();
    static ArrayList<AQV> arrayAQV = new ArrayList<>();
    static ArrayList<ADM> arrayADM = new ArrayList<>();
    static String[] cabecalho = {"ID", "IdAcesso", "Nome", "Telefone", "Email", "Imagem"};
    static String[][] matrizCadastro = {{"", ""}};
    public static String[][] matrizRegistrosDeAcesso = {{"", "", ""}};// inicia a matriz com uma linha e duas colunas com "" para que na primeira vez não apareça null na tabela de registros
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
        Student test = new Student(new User("Pedro", "9966", "9966"), "1DEV-A");
        arrayStudents.add(test);

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
                    cadastrarUsuario();
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
                    cadastrarUsuario();
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
        System.out.println("\nBem vindo " + student.user.name + "!\n1. Consultar meus dados\n2. Consultar meus atrasos");
        int menu3 = scanner.nextInt();
        switch (menu3) {
            case 1:
                System.out.println(student);
                break;
            case 2:
                student.showAccessRegisters();
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
            // Convert idAcessoRecebido to an integer
            int idAcessoInt;
            try {
                idAcessoInt = Integer.parseInt(idAcessoRecebido);
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
                    currentStudent.arrayDelays.add(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
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
            String idUsuarioEscolhido = String.valueOf(idUsuarioRecebidoPorHTTP);
            String dispositivoEscolhido = dispositivoRecebidoPorHTTP;

            if (idUsuarioRecebidoPorHTTP == 0) {
                // Exibe a lista de usuários para o administrador escolher
                for (int i = 0; i < arrayStudents.size(); i++) {
                    System.out.println(arrayStudents.get(i).toString());
                }
                // Pede ao administrador que escolha o ID do usuário
                System.out.print("Digite o ID do usuário para associar ao novo idAcesso: ");
                idUsuarioEscolhido = scanner.nextLine();
                conexaoMQTT.publicarMensagem(topico, dispositivoEscolhido);
            }
        }


            modoCadastrarIdAcesso = true;
            // Verifica se o ID do usuário existe na matriz
            for (int linhas = 1; linhas < matrizCadastro.length; linhas++) {
                if (matrizCadastro[linhas][0].equals(idUsuarioEscolhido)) { // Coluna 0 é o idUsuario
                    matrizCadastro[linhas][1] = novoIdAcesso; // Atualiza a coluna 1 com o novo idAcesso
                    System.out.println("id de acesso " + novoIdAcesso + " associado ao usuário " + matrizCadastro[linhas][2]);
                    conexaoMQTT.publicarMensagem("cadastro/disp", "CadastroConcluido");
                    encontrado = true;
                    salvarDadosNoArquivo();
                    break;
                }
            }

            // Se não encontrou o usuário, imprime uma mensagem
            if (!encontrado) {
                System.out.println("Usuário com id" + idUsuarioEscolhido + " não encontrado.");
            }
        }

        // Funções de CRUD
        private static void exibirCadastro () {
            StringBuilder tabelaCadastro = new StringBuilder();

            for (String[] usuarioLinha : matrizCadastro) {
                for (int colunas = 0; colunas < matrizCadastro[0].length; colunas++) {
                    int largura = colunas < 2 ? (colunas == 0 ? 4 : 8) : 25;
                    tabelaCadastro.append(String.format("%-" + largura + "s | ", usuarioLinha[colunas]));
                }
                tabelaCadastro.append("\n");
            }
            System.out.println(tabelaCadastro);
        }

        private static void cadastrarUsuario () {
            System.out.print("Digite a quantidade de usuarios que deseja cadastrar:");
            int qtdUsuarios = scanner.nextInt();
            scanner.nextLine();

            String[][] novaMatriz = new String[matrizCadastro.length + qtdUsuarios][matrizCadastro[0].length];

            for (int linhas = 0; linhas < matrizCadastro.length; linhas++) {
                novaMatriz[linhas] = Arrays.copyOf(matrizCadastro[linhas], matrizCadastro[linhas].length);
            }

            System.out.println("\nPreencha os dados a seguir:");
            for (int linhas = matrizCadastro.length; linhas < novaMatriz.length; linhas++) {
                System.out.println(matrizCadastro[0][0] + "- " + linhas);
                novaMatriz[linhas][0] = String.valueOf(linhas);// preenche o campo id com o numero gerado pelo for
                novaMatriz[linhas][1] = "-"; //preenche o campo idCadastro com "-"

                for (int colunas = 2; colunas < matrizCadastro[0].length - 1; colunas++) {
                    System.out.print(matrizCadastro[0][colunas] + ": ");
                    novaMatriz[linhas][colunas] = scanner.nextLine();
                }
                novaMatriz[linhas][matrizCadastro[0].length - 1] = "-";//preenche o campo imagem com "-"

                System.out.println("-----------------------Inserido com sucesso------------------------\n");
            }
            matrizCadastro = novaMatriz;
            salvarDadosNoArquivo();
        }

        private static void atualizarUsuario () {

            exibirCadastro();
            System.out.println("Escolha um id para atualizar o cadastro:");
            int idUsuario = scanner.nextInt();
            scanner.nextLine();
            System.out.println("\nAtualize os dados a seguir:");

            System.out.println(matrizCadastro[0][0] + "- " + idUsuario);
            for (int dados = 2; dados < matrizCadastro[0].length; dados++) {
                System.out.print(matrizCadastro[0][dados] + ": ");
                matrizCadastro[idUsuario][dados] = scanner.nextLine();
            }

            System.out.println("---------Atualizado com sucesso-----------");
            exibirCadastro();
            salvarDadosNoArquivo();
        }

        public static void deletarUsuario () {
            String[][] novaMatriz = new String[matrizCadastro.length - 1][matrizCadastro[0].length];
            int idUsuario = idUsuarioRecebidoPorHTTP;
            if (idUsuarioRecebidoPorHTTP == 0) {
                exibirCadastro();
                System.out.println("Escolha um id para deletar o cadastro:");
                idUsuario = scanner.nextInt();
                scanner.nextLine();
            }

            for (int i = 1, j = 1; i < matrizCadastro.length; i++) {
                if (i == idUsuario)
                    continue;
                novaMatriz[j] = matrizCadastro[i];
                novaMatriz[j][0] = String.valueOf(j);
                j++;
            }

            matrizCadastro = novaMatriz;
            matrizCadastro[0] = cabecalho;
            salvarDadosNoArquivo();
            System.out.println("-----------------------Deletado com sucesso------------------------\n");
            idUsuarioRecebidoPorHTTP = 0;
        }

        // Funções para persistência de dados
        private static void carregarDadosDoArquivo () {

            try (BufferedReader reader = new BufferedReader(new FileReader(arquivoBancoDeDados))) {
                String linha;
                StringBuilder conteudo = new StringBuilder();

                while ((linha = reader.readLine()) != null) {
                    if (!linha.trim().isEmpty()) {
                        conteudo.append(linha).append("\n");
                    }
                }

                if (!conteudo.toString().trim().isEmpty()) {
                    String[] linhasDaTabela = conteudo.toString().split("\n");
                    matrizCadastro = new String[linhasDaTabela.length][cabecalho.length];
                    for (int i = 0; i < linhasDaTabela.length; i++) {
                        matrizCadastro[i] = linhasDaTabela[i].split(",");
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            matrizCadastro[0] = cabecalho;
        }

        public static void salvarDadosNoArquivo () {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivoBancoDeDados))) {
                for (String[] linha : matrizCadastro) {
                    writer.write(String.join(",", linha) + "\n");
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

            // Verifica se o arquivo bancoDeDados.txt existe, caso contrário, cria
            if (!arquivoBancoDeDados.exists()) {
                try {
                    if (arquivoBancoDeDados.createNewFile()) {
                        System.out.println("Arquivo bancoDeDados.txt criado com sucesso.");
                    } else {
                        System.out.println("Falha ao criar o arquivo bancoDeDados.txt.");
                    }
                } catch (IOException e) {
                    System.out.println("Erro ao criar arquivo bancoDeDados.txt: " + e.getMessage());
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
