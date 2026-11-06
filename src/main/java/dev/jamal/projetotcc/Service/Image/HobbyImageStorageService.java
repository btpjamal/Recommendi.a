package dev.jamal.projetotcc.Service.Image;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import dev.jamal.projetotcc.DTO.Image.HobbyImageUploadResponseDTO;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HobbyImageStorageService {

    private static final long TAMANHO_MAXIMO = 5 * 1024 * 1024;

    private static final Set<String> TIPOS_PERMITIDOS =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final Cloudinary cloudinary;

    public HobbyImageUploadResponseDTO enviarImagem(MultipartFile arquivo) throws IOException {

        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException(
                    "Selecione uma imagem para enviar."
            );
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException(
                    "A imagem deve ter no máximo 5 MB."
            );
        }

        if (!TIPOS_PERMITIDOS.contains(arquivo.getContentType())) {
            throw new IllegalArgumentException(
                    "Formato inválido. Utilize JPG, PNG ou WebP."
            );
        }

        Map<?, ?> resultado = cloudinary.uploader().upload(
                arquivo.getBytes(),
                ObjectUtils.asMap(
                        "folder", "recommendia/hobbies",
                        "resource_type", "image"
                )
        );

        Object url = resultado.get("secure_url");
        Object publicId = resultado.get("public_id");

        if (url == null || publicId == null) {
            throw new IOException(
                    "O Cloudinary não retornou os dados da imagem."
            );
        }

        return new HobbyImageUploadResponseDTO(
                url.toString(),
                publicId.toString()
        );
    }

    public void excluirImagem(String publicId) throws IOException {

        if (publicId == null ||
                !publicId.startsWith("recommendia/hobbies/")) {
            throw new IllegalArgumentException(
                    "Identificador de imagem inválido."
            );
        }

        Map<?, ?> resultado = cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.emptyMap()
        );

        if (!"ok".equals(resultado.get("result"))) {
            throw new IOException(
                    "Não foi possível excluir a imagem do Cloudinary."
            );
        }
    }
}