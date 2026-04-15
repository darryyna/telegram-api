package edu.lytvyniuk.telegrambot.DTO;

/*
  @author darin
  @project telegram-bot
  @class NlpResult
  @version 1.0.0
  @since 08.04.2026 - 22.02
*/

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NlpResult {
    private UserIntent intent;
    private String location;
    private String currencyCode;
    private String newsCategory;
}
