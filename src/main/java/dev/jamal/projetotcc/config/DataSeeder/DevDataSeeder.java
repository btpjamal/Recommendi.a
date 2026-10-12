package dev.jamal.projetotcc.config.DataSeeder;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.AmbientePreferido;
import dev.jamal.projetotcc.Enum.NivelAtividadeFisica;
import dev.jamal.projetotcc.Enum.TipoSocializacao;

import dev.jamal.projetotcc.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
@RequiredArgsConstructor
public class DevDataSeeder {

    private final SeederMethods seederMethods;

    @Bean
    CommandLineRunner seedDatabase(){
        return args -> {

            Interesses interesses = seedInteresses();
            Categorias categorias = seedCategorias();
            Objetivos objetivos = seedObjetivos();


            seedEsportes(categorias, interesses, objetivos);
            seedCriatividade(categorias, interesses, objetivos);
            seedIntelectual(categorias, interesses, objetivos);
            seedSocial(categorias, interesses, objetivos);
            seedTecnologia(categorias, interesses, objetivos);
            seedRelaxamento(categorias, interesses, objetivos);


            System.out.println("Seed finalizado");
        };
    }


    private record Interesses(
            Interest esportes,
            Interest musica,
            Interest artesVisuais,
            Interest tecnologia,
            Interest jogos,
            Interest natureza,
            Interest literatura,
            Interest culinaria,
            Interest aprendizado,
            Interest criacao,
            Interest cultura,
            Interest socializacao,
            Interest relaxar
    ) {
    }

    private record Categorias(
            HobbyCategory esporte,
            HobbyCategory criatividade,
            HobbyCategory intelectual,
            HobbyCategory social,
            HobbyCategory tecnologia,
            HobbyCategory relaxamento
    ) {
    }

    private record Objetivos(
            Objective relaxar,
            Objective conhecerPessoas,
            Objective aprender,
            Objective condicionamento,
            Objective criatividade,
            Objective disciplina,
            Objective reduzirEstresse,
            Objective diversao,
            Objective produzir,
            Objective competir
    ) {
    }

    private Interesses seedInteresses() {

        return new Interesses(
                seederMethods.obterOuCriarInteresse("Esportes"),
                seederMethods.obterOuCriarInteresse("Música"),
                seederMethods.obterOuCriarInteresse("Artes visuais"),
                seederMethods.obterOuCriarInteresse("Tecnologia"),
                seederMethods.obterOuCriarInteresse("Jogos"),
                seederMethods.obterOuCriarInteresse("Natureza"),
                seederMethods.obterOuCriarInteresse("Leitura e literatura"),
                seederMethods.obterOuCriarInteresse("Culinária"),
                seederMethods.obterOuCriarInteresse("Aprendizado"),
                seederMethods.obterOuCriarInteresse("Criação"),
                seederMethods.obterOuCriarInteresse("Cultura e expressão"),
                seederMethods.obterOuCriarInteresse("Atividades sociais"),
                seederMethods.obterOuCriarInteresse("Relaxar")
        );
    }

    private Categorias seedCategorias() {

        return new Categorias(
                seederMethods.criarCategoriaSeNaoExistir("Esporte"),
                seederMethods.criarCategoriaSeNaoExistir("Criatividade"),
                seederMethods.criarCategoriaSeNaoExistir("Intelectual"),
                seederMethods.criarCategoriaSeNaoExistir("Social"),
                seederMethods.criarCategoriaSeNaoExistir("Tecnologia"),
                seederMethods.criarCategoriaSeNaoExistir("Relaxamento")
        );
    }

    private Objetivos seedObjetivos() {

        return new Objetivos(

                seederMethods.obterOuCriarObjetivo(
                        "Relaxar",
                        "Atividades voltadas ao descanso, tranquilidade e bem-estar."
                ),

                seederMethods.obterOuCriarObjetivo(
                        "Conhecer pessoas",
                        "Atividades que favorecem interação social e criação de vínculos."
                ),

                seederMethods.obterOuCriarObjetivo(
                        "Aprender algo novo",
                        "Atividades focadas em aprendizado e desenvolvimento de novas habilidades."
                ),

                seederMethods.obterOuCriarObjetivo(
                        "Condicionamento",
                        "Atividades que promovem esforço físico e condicionamento."
                ),

                seederMethods.obterOuCriarObjetivo(
                        "Desenvolver criatividade",
                        "Atividades que estimulam imaginação, expressão e criação."
                ),

                seederMethods.obterOuCriarObjetivo(
                        "Criar disciplina",
                        "Atividades que favorecem consistência, rotina e desenvolvimento de hábitos."
                ),

                seederMethods.obterOuCriarObjetivo(
                        "Reduzir estresse",
                        "Atividades associadas ao relaxamento e redução de tensão."
                ),

                seederMethods.obterOuCriarObjetivo(
                        "Se divertir",
                        "Atividades voltadas principalmente ao entretenimento e lazer."
                ),

                seederMethods.obterOuCriarObjetivo(
                        "Produzir algo",
                        "Atividades que resultam na criação de algo concreto ou compartilhável."
                ),

                seederMethods.obterOuCriarObjetivo(
                        "Competir",
                        "Atividades que possuem elemento competitivo ou desafios contra outras pessoas."
                )
        );
    }

    private void seedEsportes(
            Categorias categorias,
            Interesses interesses,
            Objetivos objetivos
    ) {

        Hobby corrida = seederMethods.criarOuAtualizarHobby(
                "Corrida",
                "Atividade física ao ar livre.",
                0.0,
                2,
                1.0,
                TipoSocializacao.INDIVIDUAL,
                categorias.esporte(),
                NivelAtividadeFisica.ALTO,
                AmbientePreferido.AO_AR_LIVRE
        );

        seederMethods.adicionarObjetivoAoHobby(corrida, objetivos.condicionamento(), 3);
        seederMethods.adicionarObjetivoAoHobby(corrida, objetivos.disciplina(), 2);
        seederMethods.adicionarObjetivoAoHobby(corrida, objetivos.reduzirEstresse(), 2);
        seederMethods.adicionarObjetivoAoHobby(corrida, objetivos.competir(), 1);

        seederMethods.adicionarInteresseAoHobby(corrida, interesses.esportes(), 3);
        seederMethods.adicionarInteresseAoHobby(corrida, interesses.natureza(), 2);


        Hobby futebol = seederMethods.criarOuAtualizarHobby(
                "Futebol",
                "Esporte coletivo praticado em grupo.",
                20.0,
                3,
                2.0,
                TipoSocializacao.SOCIAL,
                categorias.esporte(),
                NivelAtividadeFisica.ALTO,
                AmbientePreferido.AO_AR_LIVRE
        );

        seederMethods.adicionarObjetivoAoHobby(futebol, objetivos.conhecerPessoas(), 3);
        seederMethods.adicionarObjetivoAoHobby(futebol, objetivos.competir(), 3);
        seederMethods.adicionarObjetivoAoHobby(futebol, objetivos.condicionamento(), 2);
        seederMethods.adicionarObjetivoAoHobby(futebol, objetivos.diversao(), 2);

        seederMethods.adicionarInteresseAoHobby(futebol, interesses.esportes(), 3);
        seederMethods.adicionarInteresseAoHobby(futebol, interesses.socializacao(), 2);
        seederMethods.adicionarInteresseAoHobby(futebol, interesses.jogos(), 1);


        Hobby ciclismo = seederMethods.criarOuAtualizarHobby(
                "Ciclismo",
                "Prática de pedalar por lazer ou exercício.",
                200.0,
                3,
                2.0,
                TipoSocializacao.INDIVIDUAL,
                categorias.esporte(),
                NivelAtividadeFisica.ALTO,
                AmbientePreferido.AO_AR_LIVRE
        );

        seederMethods.adicionarObjetivoAoHobby(ciclismo, objetivos.condicionamento(), 3);
        seederMethods.adicionarObjetivoAoHobby(ciclismo, objetivos.reduzirEstresse(), 2);
        seederMethods.adicionarObjetivoAoHobby(ciclismo, objetivos.diversao(), 2);
        seederMethods.adicionarObjetivoAoHobby(ciclismo, objetivos.disciplina(), 1);

        seederMethods.adicionarInteresseAoHobby(ciclismo, interesses.esportes(), 3);
        seederMethods.adicionarInteresseAoHobby(ciclismo, interesses.natureza(), 2);


        Hobby natacao = seederMethods.criarOuAtualizarHobby(
                "Natação",
                "Atividade física praticada em piscina.",
                80.0,
                3,
                1.0,
                TipoSocializacao.INDIVIDUAL,
                categorias.esporte(),
                NivelAtividadeFisica.ALTO,
                AmbientePreferido.AMBIENTE_FECHADO
        );

        seederMethods.adicionarObjetivoAoHobby(natacao, objetivos.condicionamento(), 3);
        seederMethods.adicionarObjetivoAoHobby(natacao, objetivos.disciplina(), 2);
        seederMethods.adicionarObjetivoAoHobby(natacao, objetivos.reduzirEstresse(), 2);

        seederMethods.adicionarInteresseAoHobby(natacao, interesses.esportes(), 3);
        seederMethods.adicionarInteresseAoHobby(natacao, interesses.relaxar(), 1);
    }

    private void seedCriatividade(
            Categorias categorias,
            Interesses interesses,
            Objetivos objetivos
    ) {

        Hobby fotografia = seederMethods.criarOuAtualizarHobby(
                "Fotografia",
                "Registrar momentos, paisagens e cenas criativas.",
                50.0,
                3,
                1.5,
                TipoSocializacao.INDIVIDUAL,
                categorias.criatividade(),
                NivelAtividadeFisica.BAIXO,
                AmbientePreferido.INDIFERENTE
        );

        seederMethods.adicionarObjetivoAoHobby(fotografia, objetivos.criatividade(), 3);
        seederMethods.adicionarObjetivoAoHobby(fotografia, objetivos.produzir(), 3);
        seederMethods.adicionarObjetivoAoHobby(fotografia, objetivos.aprender(), 2);
        seederMethods.adicionarObjetivoAoHobby(fotografia, objetivos.relaxar(), 1);

        seederMethods.adicionarInteresseAoHobby(fotografia, interesses.artesVisuais(), 3);
        seederMethods.adicionarInteresseAoHobby(fotografia, interesses.natureza(), 2);
        seederMethods.adicionarInteresseAoHobby(fotografia, interesses.criacao(), 3);


        Hobby desenho = seederMethods.criarOuAtualizarHobby(
                "Desenho",
                "Prática artística utilizando lápis, papel ou ferramentas digitais.",
                20.0,
                2,
                1.0,
                TipoSocializacao.INDIVIDUAL,
                categorias.criatividade(),
                NivelAtividadeFisica.BAIXO,
                AmbientePreferido.CASA
        );

        seederMethods.adicionarObjetivoAoHobby(desenho, objetivos.criatividade(), 3);
        seederMethods.adicionarObjetivoAoHobby(desenho, objetivos.produzir(), 3);
        seederMethods.adicionarObjetivoAoHobby(desenho, objetivos.relaxar(), 2);
        seederMethods.adicionarObjetivoAoHobby(desenho, objetivos.aprender(), 1);

        seederMethods.adicionarInteresseAoHobby(desenho, interesses.artesVisuais(), 3);
        seederMethods.adicionarInteresseAoHobby(desenho, interesses.criacao(), 3);


        Hobby pintura = seederMethods.criarOuAtualizarHobby(
                "Pintura",
                "Expressão artística utilizando tintas e diferentes superfícies.",
                60.0,
                3,
                1.5,
                TipoSocializacao.INDIVIDUAL,
                categorias.criatividade(),
                NivelAtividadeFisica.BAIXO,
                AmbientePreferido.CASA
        );

        seederMethods.adicionarObjetivoAoHobby(pintura, objetivos.criatividade(), 3);
        seederMethods.adicionarObjetivoAoHobby(pintura, objetivos.produzir(), 3);
        seederMethods.adicionarObjetivoAoHobby(pintura, objetivos.relaxar(), 2);
        seederMethods.adicionarObjetivoAoHobby(pintura, objetivos.reduzirEstresse(), 2);

        seederMethods.adicionarInteresseAoHobby(pintura, interesses.artesVisuais(), 3);
        seederMethods.adicionarInteresseAoHobby(pintura, interesses.criacao(), 3);


        Hobby violao = seederMethods.criarOuAtualizarHobby(
                "Violão",
                "Aprendizado musical utilizando instrumento de cordas.",
                150.0,
                4,
                1.0,
                TipoSocializacao.INDIVIDUAL,
                categorias.criatividade,
                NivelAtividadeFisica.BAIXO,
                AmbientePreferido.CASA
        );

        seederMethods.adicionarObjetivoAoHobby(violao, objetivos.aprender(), 3);
        seederMethods.adicionarObjetivoAoHobby(violao, objetivos.criatividade(), 3);
        seederMethods.adicionarObjetivoAoHobby(violao, objetivos.produzir(), 2);
        seederMethods.adicionarObjetivoAoHobby(violao, objetivos.relaxar(), 1);

        seederMethods.adicionarInteresseAoHobby(violao, interesses.musica(), 3);
        seederMethods.adicionarInteresseAoHobby(violao, interesses.criacao(), 2);
        seederMethods.adicionarInteresseAoHobby(violao, interesses.cultura(), 2);
    }

    private void seedIntelectual(
            Categorias categorias,
            Interesses interesses,
            Objetivos objetivos
    ) {

        Hobby xadrez = seederMethods.criarOuAtualizarHobby(
                "Xadrez",
                "Jogo estratégico que estimula raciocínio lógico.",
                0.0,
                4,
                1.0,
                TipoSocializacao.INDIVIDUAL,
                categorias.intelectual(),
                NivelAtividadeFisica.BAIXO,
                AmbientePreferido.CASA
        );

        seederMethods.adicionarObjetivoAoHobby(xadrez, objetivos.aprender(), 3);
        seederMethods.adicionarObjetivoAoHobby(xadrez, objetivos.competir(), 2);
        seederMethods.adicionarObjetivoAoHobby(xadrez, objetivos.disciplina(), 2);
        seederMethods.adicionarObjetivoAoHobby(xadrez, objetivos.diversao(), 1);
        seederMethods.adicionarInteresseAoHobby(xadrez, interesses.jogos(), 3);
        seederMethods.adicionarInteresseAoHobby(xadrez, interesses.aprendizado(), 2);


        Hobby leitura = seederMethods.criarOuAtualizarHobby(
                "Leitura",
                "Hábito de ler livros, artigos e outros conteúdos.",
                30.0,
                2,
                1.0,
                TipoSocializacao.INDIVIDUAL,
                categorias.intelectual(),
                NivelAtividadeFisica.BAIXO,
                AmbientePreferido.CASA
        );

        seederMethods.adicionarObjetivoAoHobby(leitura, objetivos.aprender(), 3);
        seederMethods.adicionarObjetivoAoHobby(leitura, objetivos.relaxar(), 2);
        seederMethods.adicionarObjetivoAoHobby(leitura, objetivos.reduzirEstresse(), 1);
        seederMethods.adicionarObjetivoAoHobby(leitura, objetivos.disciplina(), 1);
        seederMethods.adicionarInteresseAoHobby(leitura, interesses.literatura(), 3);
        seederMethods.adicionarInteresseAoHobby(leitura, interesses.aprendizado(), 2);
        seederMethods.adicionarInteresseAoHobby(leitura, interesses.relaxar(), 2);


        Hobby escrita = seederMethods.criarOuAtualizarHobby(
                "Escrita",
                "Produção de textos, histórias, ideias ou reflexões.",
                0.0,
                3,
                1.0,
                TipoSocializacao.INDIVIDUAL,
                categorias.intelectual(),
                NivelAtividadeFisica.BAIXO,
                AmbientePreferido.CASA
        );

        seederMethods.adicionarObjetivoAoHobby(escrita, objetivos.criatividade(), 3);
        seederMethods.adicionarObjetivoAoHobby(escrita, objetivos.produzir(), 3);
        seederMethods.adicionarObjetivoAoHobby(escrita, objetivos.aprender(), 2);
        seederMethods.adicionarObjetivoAoHobby(escrita, objetivos.relaxar(), 1);
        seederMethods.adicionarInteresseAoHobby(escrita, interesses.literatura(), 3);
        seederMethods.adicionarInteresseAoHobby(escrita, interesses.criacao(), 3);
        seederMethods.adicionarInteresseAoHobby(escrita, interesses.cultura(), 1);


        Hobby idiomas = seederMethods.criarOuAtualizarHobby(
                "Estudo de idiomas",
                "Aprendizado e prática de uma nova língua.",
                50.0,
                4,
                1.0,
                TipoSocializacao.INDIVIDUAL,
                categorias.intelectual(),
                NivelAtividadeFisica.BAIXO,
                AmbientePreferido.CASA
        );

        seederMethods.adicionarObjetivoAoHobby(idiomas, objetivos.aprender(), 3);
        seederMethods.adicionarObjetivoAoHobby(idiomas, objetivos.disciplina(), 2);
        seederMethods.adicionarObjetivoAoHobby(idiomas, objetivos.conhecerPessoas(), 1);
        seederMethods.adicionarObjetivoAoHobby(idiomas, objetivos.produzir(), 1);
        seederMethods.adicionarInteresseAoHobby(idiomas, interesses.aprendizado(), 3);
        seederMethods.adicionarInteresseAoHobby(idiomas, interesses.cultura(), 2);
    }

    private void seedSocial(
            Categorias categorias,
            Interesses interesses,
            Objetivos objetivos
    ) {

        Hobby teatro = seederMethods.criarOuAtualizarHobby(
                "Teatro",
                "Atividade artística em grupo voltada à expressão e interpretação.",
                30.0,
                4,
                2.0,
                TipoSocializacao.SOCIAL,
                categorias.social(),
                NivelAtividadeFisica.MODERADO,
                AmbientePreferido.AMBIENTE_FECHADO
        );

        seederMethods.adicionarObjetivoAoHobby(teatro, objetivos.conhecerPessoas(), 3);
        seederMethods.adicionarObjetivoAoHobby(teatro, objetivos.criatividade(), 3);
        seederMethods.adicionarObjetivoAoHobby(teatro, objetivos.produzir(), 2);
        seederMethods.adicionarObjetivoAoHobby(teatro, objetivos.aprender(), 2);
        seederMethods.adicionarInteresseAoHobby(teatro, interesses.cultura(), 3);
        seederMethods.adicionarInteresseAoHobby(teatro, interesses.criacao(), 2);
        seederMethods.adicionarInteresseAoHobby(teatro, interesses.socializacao(), 3);


        Hobby danca = seederMethods.criarOuAtualizarHobby(
                "Dança",
                "Atividade corporal, musical e social.",
                50.0,
                3,
                1.5,
                TipoSocializacao.SOCIAL,
                categorias.social(),
                NivelAtividadeFisica.ALTO,
                AmbientePreferido.AMBIENTE_FECHADO
        );

        seederMethods.adicionarObjetivoAoHobby(danca, objetivos.diversao(), 3);
        seederMethods.adicionarObjetivoAoHobby(danca, objetivos.conhecerPessoas(), 3);
        seederMethods.adicionarObjetivoAoHobby(danca, objetivos.condicionamento(), 2);
        seederMethods.adicionarObjetivoAoHobby(danca, objetivos.criatividade(), 2);
        seederMethods.adicionarInteresseAoHobby(danca, interesses.musica(), 2);
        seederMethods.adicionarInteresseAoHobby(danca, interesses.cultura(), 2);
        seederMethods.adicionarInteresseAoHobby(danca, interesses.socializacao(), 3);


        Hobby voluntariado = seederMethods.criarOuAtualizarHobby(
                "Voluntariado",
                "Participação em ações sociais e comunitárias.",
                0.0,
                2,
                2.0,
                TipoSocializacao.SOCIAL,
                categorias.social(),
                NivelAtividadeFisica.MODERADO,
                AmbientePreferido.INDIFERENTE
        );

        seederMethods.adicionarObjetivoAoHobby(voluntariado, objetivos.conhecerPessoas(), 3);
        seederMethods.adicionarObjetivoAoHobby(voluntariado, objetivos.aprender(), 2);
        seederMethods.adicionarObjetivoAoHobby(voluntariado, objetivos.disciplina(), 1);
        seederMethods.adicionarInteresseAoHobby(voluntariado, interesses.socializacao(), 3);
        seederMethods.adicionarInteresseAoHobby(voluntariado, interesses.aprendizado(), 1);


        Hobby clubeJogos = seederMethods.criarOuAtualizarHobby(
                "Clube de jogos",
                "Encontros para jogos de tabuleiro, cartas e jogos sociais.",
                20.0,
                2,
                2.0,
                TipoSocializacao.SOCIAL,
                categorias.social(),
                NivelAtividadeFisica.BAIXO,
                AmbientePreferido.AMBIENTE_FECHADO
        );

        seederMethods.adicionarObjetivoAoHobby(clubeJogos, objetivos.conhecerPessoas(), 3);
        seederMethods.adicionarObjetivoAoHobby(clubeJogos, objetivos.diversao(), 3);
        seederMethods.adicionarObjetivoAoHobby(clubeJogos, objetivos.competir(), 2);
        seederMethods.adicionarInteresseAoHobby(clubeJogos, interesses.jogos(), 3);
        seederMethods.adicionarInteresseAoHobby(clubeJogos, interesses.socializacao(), 3);
    }

    private void seedTecnologia(
            Categorias categorias,
            Interesses interesses,
            Objetivos objetivos
    ) {

        Hobby programacaoCriativa = seederMethods.criarOuAtualizarHobby(
                "Programação criativa",
                "Criação de pequenos projetos, automações e experimentos utilizando programação.",
                0.0,
                4,
                1.5,
                TipoSocializacao.INDIVIDUAL,
                categorias.tecnologia(),
                NivelAtividadeFisica.BAIXO,
                AmbientePreferido.CASA
        );

        seederMethods.adicionarObjetivoAoHobby(programacaoCriativa, objetivos.aprender(), 3);
        seederMethods.adicionarObjetivoAoHobby(programacaoCriativa, objetivos.criatividade(), 3);
        seederMethods.adicionarObjetivoAoHobby(programacaoCriativa, objetivos.produzir(), 3);
        seederMethods.adicionarObjetivoAoHobby(programacaoCriativa, objetivos.disciplina(), 1);
        seederMethods.adicionarInteresseAoHobby(programacaoCriativa, interesses.tecnologia(), 3);
        seederMethods.adicionarInteresseAoHobby(programacaoCriativa, interesses.criacao(), 3);
        seederMethods.adicionarInteresseAoHobby(programacaoCriativa, interesses.aprendizado(), 2);


        Hobby edicaoVideo = seederMethods.criarOuAtualizarHobby(
                "Edição de vídeo",
                "Produção e edição de conteúdos audiovisuais.",
                0.0,
                3,
                1.5,
                TipoSocializacao.INDIVIDUAL,
                categorias.tecnologia(),
                NivelAtividadeFisica.BAIXO,
                AmbientePreferido.CASA
        );

        seederMethods.adicionarObjetivoAoHobby(edicaoVideo, objetivos.criatividade(), 3);
        seederMethods.adicionarObjetivoAoHobby(edicaoVideo, objetivos.produzir(), 3);
        seederMethods.adicionarObjetivoAoHobby(edicaoVideo, objetivos.aprender(), 2);
        seederMethods.adicionarInteresseAoHobby(edicaoVideo, interesses.tecnologia(), 2);
        seederMethods.adicionarInteresseAoHobby(edicaoVideo, interesses.artesVisuais(), 3);
        seederMethods.adicionarInteresseAoHobby(edicaoVideo, interesses.criacao(), 3);


        Hobby robotica = seederMethods.criarOuAtualizarHobby(
                "Robótica básica",
                "Montagem e programação de pequenos circuitos e dispositivos.",
                150.0,
                5,
                2.0,
                TipoSocializacao.INDIVIDUAL,
                categorias.tecnologia(),
                NivelAtividadeFisica.BAIXO,
                AmbientePreferido.CASA
        );

        seederMethods.adicionarObjetivoAoHobby(robotica, objetivos.aprender(), 3);
        seederMethods.adicionarObjetivoAoHobby(robotica, objetivos.produzir(), 3);
        seederMethods.adicionarObjetivoAoHobby(robotica, objetivos.criatividade(), 2);
        seederMethods.adicionarObjetivoAoHobby(robotica, objetivos.disciplina(), 2);
        seederMethods.adicionarInteresseAoHobby(robotica, interesses.tecnologia(), 3);
        seederMethods.adicionarInteresseAoHobby(robotica, interesses.aprendizado(), 3);
        seederMethods.adicionarInteresseAoHobby(robotica, interesses.criacao(), 2);


        Hobby criacaoJogos = seederMethods.criarOuAtualizarHobby(
                "Criação de jogos",
                "Desenvolvimento de jogos simples e interativos.",
                0.0,
                4,
                2.0,
                TipoSocializacao.INDIVIDUAL,
                categorias.tecnologia(),
                NivelAtividadeFisica.BAIXO,
                AmbientePreferido.CASA
        );

        seederMethods.adicionarObjetivoAoHobby(criacaoJogos, objetivos.criatividade(), 3);
        seederMethods.adicionarObjetivoAoHobby(criacaoJogos, objetivos.produzir(), 3);
        seederMethods.adicionarObjetivoAoHobby(criacaoJogos, objetivos.aprender(), 3);
        seederMethods.adicionarObjetivoAoHobby(criacaoJogos, objetivos.diversao(), 2);
        seederMethods.adicionarInteresseAoHobby(criacaoJogos, interesses.tecnologia(), 3);
        seederMethods.adicionarInteresseAoHobby(criacaoJogos, interesses.jogos(), 3);
        seederMethods.adicionarInteresseAoHobby(criacaoJogos, interesses.criacao(), 3);
    }

    private void seedRelaxamento(
            Categorias categorias,
            Interesses interesses,
            Objetivos objetivos
    ) {

        Hobby meditacao = seederMethods.criarOuAtualizarHobby(
                "Meditação",
                "Prática de atenção plena, respiração e relaxamento.",
                0.0,
                1,
                0.5,
                TipoSocializacao.INDIVIDUAL,
                categorias.relaxamento(),
                NivelAtividadeFisica.BAIXO,
                AmbientePreferido.CASA
        );

        seederMethods.adicionarObjetivoAoHobby(meditacao, objetivos.relaxar(), 3);
        seederMethods.adicionarObjetivoAoHobby(meditacao, objetivos.reduzirEstresse(), 3);
        seederMethods.adicionarObjetivoAoHobby(meditacao, objetivos.disciplina(), 1);
        seederMethods.adicionarInteresseAoHobby(meditacao, interesses.relaxar(), 3);


        Hobby jardinagem = seederMethods.criarOuAtualizarHobby(
                "Jardinagem",
                "Cultivo e cuidado de plantas, flores e pequenos jardins.",
                40.0,
                2,
                1.0,
                TipoSocializacao.INDIVIDUAL,
                categorias.relaxamento(),
                NivelAtividadeFisica.MODERADO,
                AmbientePreferido.AO_AR_LIVRE
        );

        seederMethods.adicionarObjetivoAoHobby(jardinagem, objetivos.relaxar(), 3);
        seederMethods.adicionarObjetivoAoHobby(jardinagem, objetivos.produzir(), 2);
        seederMethods.adicionarObjetivoAoHobby(jardinagem, objetivos.reduzirEstresse(), 2);
        seederMethods.adicionarObjetivoAoHobby(jardinagem, objetivos.aprender(), 1);
        seederMethods.adicionarInteresseAoHobby(jardinagem, interesses.natureza(), 3);
        seederMethods.adicionarInteresseAoHobby(jardinagem, interesses.relaxar(), 2);
        seederMethods.adicionarInteresseAoHobby(jardinagem, interesses.criacao(), 1);


        Hobby culinaria = seederMethods.criarOuAtualizarHobby(
                "Culinária",
                "Preparação de receitas e experimentação gastronômica.",
                50.0,
                3,
                1.5,
                TipoSocializacao.INDIVIDUAL,
                categorias.relaxamento(),
                NivelAtividadeFisica.BAIXO,
                AmbientePreferido.CASA
        );

        seederMethods.adicionarObjetivoAoHobby(culinaria, objetivos.produzir(), 3);
        seederMethods.adicionarObjetivoAoHobby(culinaria, objetivos.criatividade(), 2);
        seederMethods.adicionarObjetivoAoHobby(culinaria, objetivos.aprender(), 2);
        seederMethods.adicionarObjetivoAoHobby(culinaria, objetivos.diversao(), 2);
        seederMethods.adicionarObjetivoAoHobby(culinaria, objetivos.relaxar(), 1);
        seederMethods.adicionarInteresseAoHobby(culinaria, interesses.culinaria(), 3);
        seederMethods.adicionarInteresseAoHobby(culinaria, interesses.criacao(), 2);
        seederMethods.adicionarInteresseAoHobby(culinaria, interesses.aprendizado(), 1);


        Hobby caminhada = seederMethods.criarOuAtualizarHobby(
                "Caminhada",
                "Atividade física leve praticada principalmente ao ar livre.",
                0.0,
                1,
                1.0,
                TipoSocializacao.INDIVIDUAL,
                categorias.relaxamento(),
                NivelAtividadeFisica.MODERADO,
                AmbientePreferido.AO_AR_LIVRE
        );

        seederMethods.adicionarObjetivoAoHobby(caminhada, objetivos.relaxar(), 3);
        seederMethods.adicionarObjetivoAoHobby(caminhada, objetivos.reduzirEstresse(), 3);
        seederMethods.adicionarObjetivoAoHobby(caminhada, objetivos.condicionamento(), 2);
        seederMethods.adicionarInteresseAoHobby(caminhada, interesses.relaxar(), 3);
    }

}
