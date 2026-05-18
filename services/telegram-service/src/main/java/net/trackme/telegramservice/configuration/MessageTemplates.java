package net.trackme.telegramservice.configuration;

public class MessageTemplates {
    /**
     * Шаблон начального сообщения.
     */
    public static String START_MESSAGE_TEMPLATE = """
                                  Вас приветствует бот оповещений TrackMe.
                                  Я буду присылать вам оповещения о пропущенных встречах.
                                  """;

    /**
     * Шаблон сообщения о пропущенной встрече.
     */
    public static String MEETING_NOT_HAPPENED_MESSAGE_TEMPLATE = """
                                               Здравствуйте, {trackerFullName}.
                                               Назначенная встреча не состоялась!
            
                                               Ваша команда {teamCardName} с потока {streamName} пропустила назначенную встречу.
                                               Вы можете узнать информацию о пропущенной встрече, перейдя по ссылке ниже:
                                               {meetingLink}
                                               """;
}
