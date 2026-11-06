package dev.jamal.projetotcc.Service.Image;

import dev.jamal.projetotcc.DTO.Image.HobbyImageUploadResponseDTO;
import dev.jamal.projetotcc.Entities.HobbyImageUpload;
import dev.jamal.projetotcc.Enum.HobbyImageStatus;
import dev.jamal.projetotcc.Repository.HobbyImageUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import dev.jamal.projetotcc.Entities.Hobby;
import dev.jamal.projetotcc.Repository.HobbyRepository;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class HobbyImageUploadService {

    private final HobbyImageStorageService storageService;
    private final HobbyImageUploadRepository uploadRepository;
    private final HobbyRepository hobbyRepository;

    public HobbyImageUploadResponseDTO enviarImagem(
            MultipartFile arquivo
    ) throws IOException {

        // 1. Envia a imagem ao Cloudinary
        HobbyImageUploadResponseDTO resultado =
                storageService.enviarImagem(arquivo);

        // 2. Prepara o registro no PostgreSQL
        HobbyImageUpload upload = new HobbyImageUpload();

        upload.setImagemUrl(resultado.imagemUrl());
        upload.setPublicId(resultado.publicId());
        upload.setStatus(HobbyImageStatus.PENDENTE);

        // 3. Persiste o registro
        try {
            uploadRepository.saveAndFlush(upload);
        } catch (RuntimeException erro) {

            // Se o banco falhar, tenta remover o arquivo enviado
            try {
                storageService.excluirImagem(resultado.publicId());
            } catch (Exception erroExclusao) {
                erro.addSuppressed(erroExclusao);
            }

            throw erro;
        }

        return resultado;
    }

    @Transactional
    public void confirmarImagem(String publicId, Long hobbyId) {

        // 1. Localiza o upload
        HobbyImageUpload upload = uploadRepository
                .findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Imagem não encontrada."
                ));

        // 2. Verifica se ainda está pendente
        if (upload.getStatus() != HobbyImageStatus.PENDENTE) {
            throw new IllegalStateException(
                    "A imagem não está pendente de confirmação."
            );
        }

        // 3. Localiza o hobby
        Hobby hobby = hobbyRepository.findById(hobbyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Hobby não encontrado."
                ));

        // 4. Verifica se a imagem já está vinculada
        if (hobbyRepository.existsByImagemUrl(upload.getImagemUrl())) {
            throw new IllegalStateException(
                    "Esta imagem já está vinculada a um hobby."
            );
        }

        // 5. Associa a imagem ao hobby
        hobby.setImagemUrl(upload.getImagemUrl());

        // 6. Atualiza o registro do upload
        upload.setHobby(hobby);
        upload.setStatus(HobbyImageStatus.CONFIRMADA);
    }

    @Transactional
    public void descartarImagem(String publicId) throws IOException {

        // 1. Localiza e bloqueia o registro
        HobbyImageUpload upload = uploadRepository
                .findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Imagem não encontrada."
                ));

        // 2. Verifica o status
        if (upload.getStatus() != HobbyImageStatus.PENDENTE) {
            throw new IllegalStateException(
                    "Somente imagens pendentes podem ser descartadas."
            );
        }

        // 3. Verifica se existe vínculo com algum hobby
        if (upload.getHobby() != null ||
                hobbyRepository.existsByImagemUrl(upload.getImagemUrl())) {
            throw new IllegalStateException(
                    "A imagem está vinculada a um hobby."
            );
        }

        // 4. Exclui o arquivo do Cloudinary
        storageService.excluirImagem(upload.getPublicId());

        // 5. Atualiza o status no PostgreSQL
        upload.setStatus(HobbyImageStatus.DESCARTADA);
        uploadRepository.saveAndFlush(upload);
    }
}