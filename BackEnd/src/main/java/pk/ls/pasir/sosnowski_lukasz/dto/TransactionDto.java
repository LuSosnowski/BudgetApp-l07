package pk.ls.pasir.sosnowski_lukasz.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionDto {

    @NotNull(message = "Kwota nie moze byc pusta")
    @DecimalMin(value = "0.01", message = "Kwota musi byc wieksza od 0")
    private Double amount;

    @NotNull(message = "Typ transakcji jest wymagany")
    @Pattern(regexp = "INCOME|EXPENSE", message = "Typ transakcji musi byc wartoscia INCOME lub EXPENSE")
    private String type;

    @Size(max = 70, message = "Tagi nie moga przekraczac 70 znakow")
    private String tags;

    @Size(max = 200, message = "Notatka moze miec maksymalnie 200 znakow")
    private String notes;
}