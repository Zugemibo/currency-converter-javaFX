package pl.piatekd;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import pl.piatekd.model.CurrencyShortName;
import pl.piatekd.utils.ComboBoxAutoComplete;

public class FxmlController implements Initializable {

    @FXML
    private JFXTextField tfQuantity;

    @FXML
    private ComboBox<CurrencyShortName> cbBaseCurrency;

    @FXML
    private ComboBox<CurrencyShortName> cbOutputCurrency;

    @FXML
    private JFXButton btnCalculate;

    @FXML
    private Label calculatedValue;

    @FXML
    private JFXButton btnClose;

    @FXML
    void cbBaseCurrencyValueHandler(ActionEvent event) {
        System.out.println("Successfully changed base currency to: " + cbBaseCurrency.getValue());
    }

    @FXML
    void cbOutputCurrencyValueHandler(ActionEvent event) {
        System.out.println("Successfully changed output currency to: " + cbOutputCurrency.getValue());
    }

    private Map<CurrencyShortName, BigDecimal> rates = new HashMap<>();

    private CurrencyShortName baseName;

    private void setUp(Map<CurrencyShortName, BigDecimal> map) {
        this.rates = map;
        this.baseName = CurrencyShortName.EUR;
        System.out.println("Base currency is: " + baseName);
    }

    @FXML
    private void handleButtonAction(ActionEvent event) {
        if (event.getSource() == btnClose) {
            closeApplication();
        } else if (event.getSource() == btnCalculate) {
            String calculated = String.valueOf(calculate());
            StringBuilder sb = new StringBuilder();
            sb.append(tfQuantity.getText())
                    .append(" ")
                    .append(cbBaseCurrency.getValue())
                    .append(" = ")
                    .append(calculated)
                    .append(cbOutputCurrency.getValue());
            calculatedValue.setText(sb.toString());
        }
    }

    public BigDecimal calculate() {
        BigDecimal calculatedAmount = BigDecimal.ZERO;
        if (cbBaseCurrency.getValue() == CurrencyShortName.EUR) {
            calculatedAmount = rates.get(cbOutputCurrency.getValue()).multiply(new BigDecimal(tfQuantity.getText()));
        } else {
            calculatedAmount = rates.get(cbOutputCurrency.getValue()).divide(rates.get(cbBaseCurrency.getValue()), 6, RoundingMode.HALF_EVEN).multiply(new BigDecimal(tfQuantity.getText()));
        }
        return calculatedAmount;
    }

    private void closeApplication() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();

    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        setTextFieldNumericOnly(tfQuantity);
        setComboBoxValuesAndProperties(cbBaseCurrency);
        setComboBoxValuesAndProperties(cbOutputCurrency);
        try {
            getData();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected void setTextFieldNumericOnly(JFXTextField textField) {
        textField.textProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                                String newValue) {
                if (!newValue.matches("\\d*")) {
                    textField.setText(newValue.replaceAll("[^\\d]", ""));
                }
            }
        });
    }

    protected void setComboBoxValuesAndProperties(ComboBox<CurrencyShortName> comboBox) {
        comboBox.setTooltip(new Tooltip());
        comboBox.getItems().setAll(CurrencyShortName.values());
        new ComboBoxAutoComplete<>(comboBox);
    }

    public void getData() throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient().newBuilder().build();
                Request request = new Request.Builder()
                        .url("http://data.fixer.io/api/latest?access_key=bff96d14081af2aa11f6fbae26b53739")
                        .method("GET", null).addHeader("Cookie", "__cfduid=df700d8afd5da70fa34ed920be481855f1617860373")
                        .build();
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    ResponseBody responseBody = client.newCall(request).execute().body();
                    String responseBodyToString = responseBody.string();
                    JsonNode node = mapper.readTree(responseBodyToString);
                    Gson gson = new Gson();
                    String string = node.get("rates").toString();
                    Type type = new TypeToken<HashMap<CurrencyShortName, BigDecimal>>() {
                    }.getType();
                    HashMap<CurrencyShortName, BigDecimal> rates = gson.fromJson(string, type);
                    setUp(rates);
                    System.out.println("All rates in EUR: " + rates);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
