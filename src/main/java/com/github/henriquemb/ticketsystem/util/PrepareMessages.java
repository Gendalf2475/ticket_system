package com.github.henriquemb.ticketsystem.util;

import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.database.model.ReportModel;
import com.github.henriquemb.ticketsystem.database.model.SuggestionModel;
import com.github.henriquemb.ticketsystem.database.model.TicketModel;
import com.github.henriquemb.ticketsystem.enums.ReportStatusEnum;
import com.github.henriquemb.ticketsystem.enums.TicketRatingEnum;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Arrays;

public class PrepareMessages {
    private final FileConfiguration messages = TicketSystem.getMessages();

    public String reportViewMessage(String msg, ReportModel report) {
        ReportStatusEnum status = null;
        for (ReportStatusEnum r : ReportStatusEnum.values()) {
            if (r.getId() == report.getStatus()) {
                status = r;
                break;
            }
        }

        String reason = report.getReason();
        if (reason == null || reason.equals("")) reason = messages.getString("report.no_reason");
        String reasonShot = reason.split(" ").length > 3 ? String.join(" ", Arrays.copyOfRange(reason.split(" "), 0, 3)) + ".." : reason;

        return msg.replace("<id>", report.getId().toString())
                .replace("<player>", report.getPlayer())
                .replace("<reported>", report.getReported())
                .replace("<reason>", reason)
                .replace("<reason-shot>", reasonShot)
                .replace("<evidence>", verifyNull(report.getEvidence(), messages.getString("report.no_reason")))
                .replace("<status>", status.format())
                .replace("<button-teleport>",
                        String.format("[%s](/report teleport %d hover=%s)",
                                messages.getString("report.buttons.teleport.label"),
                                report.getId(),
                                messages.getString("report.buttons.teleport.hover")))
                .replace("<button-view>",
                        String.format("[%s](/report view %d hover=%s)",
                                messages.getString("report.buttons.view.label"),
                                report.getId(),
                                messages.getString("report.buttons.view.hover")))
                .replace("<button-list>",
                        String.format("[%s](/reports hover=%s)",
                                messages.getString("report.buttons.list.label"),
                                messages.getString("report.buttons.list.hover")));
    }

    public String reportViewResponseMessage(String msg, ReportModel report) {
        ReportStatusEnum status = null;
        for (ReportStatusEnum r : ReportStatusEnum.values()) {
            if (r.getId() == report.getStatus()) {
                status = r;
                break;
            }
        }

        String reason = report.getReason();
        if (reason == null || reason.equals("")) reason = messages.getString("report.no_reason");
        String reasonShot = reason.split(" ").length > 3 ? String.join(" ", Arrays.copyOfRange(reason.split(" "), 0, 3)) + ".." : reason;

        return msg.replace("<id>", report.getId().toString())
                .replace("<player>", report.getPlayer())
                .replace("<reported>", report.getReported())
                .replace("<reason>", reason)
                .replace("<reason-shot>", reasonShot)
                .replace("<evidence>", verifyNull(report.getEvidence(), messages.getString("report.no_reason")))
                .replace("<status>", status.format());
    }

    public String suggestionViewMessage(String msg, SuggestionModel suggestion) {
        String suggestionShot = suggestion.getSuggestion();
        if (suggestionShot.split(" ").length > 3) suggestionShot = String.join(" ", Arrays.copyOfRange(suggestionShot.split(" "), 0, 3)) + "..";

        return msg.replace("<id>", suggestion.getId().toString())
                .replace("<player>", suggestion.getPlayer())
                .replace("<suggestion>", suggestion.getSuggestion())
                .replace("<suggestion-shot>", suggestionShot)
                .replace("<respondedBy>", verifyNull(suggestion.getRespondedBy(), ""))
                .replace("<response>", verifyNull(suggestion.getResponse(), ""))
                .replace("<staffer>", verifyNull(suggestion.getRespondedBy(), ""))
                .replace("<button-response>",
                        String.format("[%s](suggest_command=/suggestion response %d hover=%s)",
                                messages.getString("suggestion.buttons.response.label"),
                                suggestion.getId(),
                                messages.getString("suggestion.buttons.response.hover")))
                .replace("<button-view>",
                        String.format("[%s](/suggestion view %d hover=%s)",
                                messages.getString("suggestion.buttons.view.label"),
                                suggestion.getId(),
                                messages.getString("suggestion.buttons.view.hover")))
                .replace("<button-list>",
                        String.format("[%s](/suggestions hover=%s)",
                                messages.getString("suggestion.buttons.list.label"),
                                messages.getString("suggestion.buttons.list.hover")));
    }

    public String suggestionViewResponseMessage(String msg, SuggestionModel suggestion) {
        String suggestionShot = suggestion.getSuggestion();
        if (suggestionShot.split(" ").length > 3) suggestionShot = String.join(" ", Arrays.copyOfRange(suggestionShot.split(" "), 0, 3)) + "..";

        return msg.replace("<id>", suggestion.getId().toString())
                .replace("<player>", suggestion.getPlayer())
                .replace("<suggestion>", suggestion.getSuggestion())
                .replace("<suggestion-shot>", suggestionShot)
                .replace("<respondedBy>", verifyNull(suggestion.getRespondedBy(), ""))
                .replace("<response>", verifyNull(suggestion.getResponse(), ""))
                .replace("<staffer>", verifyNull(suggestion.getRespondedBy(), ""));
    }

    public String ticketViewMessage(String msg, TicketModel ticket) {
        TicketRatingEnum rating = getTicketRating(ticket.getRating().intValue());

        String request = ticket.getRequest();
        if (request.split(" ").length > 3) request = String.join(" ", Arrays.copyOfRange(request.split(" "), 0, 3)) + "..";

        return msg.replace("<id>", ticket.getId().toString())
                .replace("<player>", ticket.getPlayer())
                .replace("<ticket>", ticket.getRequest())
                .replace("<ticket-shot>", request)
                .replace("<response>", verifyNull(ticket.getResponse(), ""))
                .replace("<respondedBy>", verifyNull(ticket.getRespondedBy(), ""))
                .replace("<rating>", rating.format())
                .replace("<button-teleport>",
                        String.format("[%s](/ticket teleport %d hover=%s)",
                                messages.getString("ticket.buttons.teleport.label"),
                                ticket.getId(),
                                messages.getString("ticket.buttons.teleport.hover")))
                .replace("<button-view>",
                        String.format("[%s](/ticket view %d hover=%s)",
                                messages.getString("ticket.buttons.view.label"),
                                ticket.getId(),
                                messages.getString("ticket.buttons.view.hover")))
                .replace("<button-list>",
                        String.format("[%s](/tickets hover=%s)",
                                messages.getString("ticket.buttons.list.label"),
                                messages.getString("ticket.buttons.list.hover")))
                .replace("<button-response>",
                        String.format("[%s](suggest_command=/ticket response %d  hover=%s)",
                                messages.getString("ticket.buttons.response.label"),
                                ticket.getId(),
                                messages.getString("ticket.buttons.response.hover")));
    }

    public String ticketViewResponseMessage(String msg, TicketModel ticket) {
        TicketRatingEnum rating = getTicketRating(ticket.getRating().intValue());

        String request = ticket.getRequest();
        if (request.split(" ").length > 3) request = String.join(" ", Arrays.copyOfRange(request.split(" "), 0, 3)) + "..";

        return msg.replace("<id>", ticket.getId().toString())
                .replace("<player>", ticket.getPlayer())
                .replace("<ticket>", ticket.getRequest())
                .replace("<ticket-shot>", request)
                .replace("<response>", verifyNull(ticket.getResponse(), ""))
                .replace("<respondedBy>", verifyNull(ticket.getRespondedBy(), ""))
                .replace("<rating>", rating.format())
                .replace("<button-invalid_rating>", TicketRatingEnum.CANCELED.getRate() != ticket.getRating() && TicketRatingEnum.UNAVAILABLE.getRate() != ticket.getRating() ?
                        String.format("[%s](/ticket rate %d CANCELED hover=%s)",
                                messages.getString("ticket.buttons.invalid_rating.label"),
                                ticket.getId(),
                                messages.getString("ticket.buttons.invalid_rating.hover")) : "");
    }

    private static String verifyNull(String msg, String replace) {
        if (msg != null) return msg;
        return replace;
    }

    private TicketRatingEnum getTicketRating(int rating) {
        TicketRatingEnum r = null;
        for (TicketRatingEnum tre : TicketRatingEnum.values()) {
            if (tre.getRate() == rating) {
                r = tre;
                break;
            }
        }

        return r;
    }
}
