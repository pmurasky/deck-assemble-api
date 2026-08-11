package com.deckassemble.collections.api.trading;

import com.deckassemble.collections.api.trading.TradeListRequest.TradeListItemRequest;
import com.deckassemble.collections.application.trading.TradeListService;
import com.deckassemble.collections.application.trading.TradeListService.TradeListCommand;
import com.deckassemble.collections.application.trading.TradeListService.TradeListItemCommand;
import com.deckassemble.collections.application.trading.TradeMatchService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TradeListController {

    private final TradeListService tradeListService;
    private final TradeMatchService matchService;

    public TradeListController(TradeListService tradeListService, TradeMatchService matchService) {
        this.tradeListService = tradeListService;
        this.matchService = matchService;
    }

    @GetMapping("/trade-lists")
    public List<TradeListResponse> list() {
        return tradeListService.list().stream().map(TradeListResponse::from).toList();
    }

    @PostMapping("/trade-lists")
    public ResponseEntity<TradeListResponse> create(@Valid @RequestBody TradeListRequest request) {
        TradeListResponse response =
                TradeListResponse.from(tradeListService.create(command(request)));
        return ResponseEntity.created(URI.create("/trade-lists/" + response.id())).body(response);
    }

    @GetMapping("/trade-lists/{id}")
    public TradeListResponse get(@PathVariable long id) {
        return TradeListResponse.from(tradeListService.get(id));
    }

    @PutMapping("/trade-lists/{id}")
    public TradeListResponse update(
            @PathVariable long id, @Valid @RequestBody TradeListRequest request) {
        return TradeListResponse.from(tradeListService.update(id, command(request)));
    }

    @DeleteMapping("/trade-lists/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        tradeListService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trade-lists/match")
    public TradeMatchResponse match(@RequestParam long leftListId, @RequestParam long rightListId) {
        return TradeMatchResponse.from(matchService.compare(leftListId, rightListId));
    }

    private TradeListCommand command(TradeListRequest request) {
        return new TradeListCommand(
                request.name(),
                request.type(),
                request.visibility(),
                itemCommands(request.items()));
    }

    private List<TradeListItemCommand> itemCommands(List<TradeListItemRequest> items) {
        return items.stream()
                .map(
                        item ->
                                new TradeListItemCommand(
                                        item.cardPrintingId(),
                                        item.quantity(),
                                        item.condition(),
                                        item.finish(),
                                        item.language()))
                .toList();
    }
}
