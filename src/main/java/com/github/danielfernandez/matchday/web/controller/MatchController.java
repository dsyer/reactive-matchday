/*
 * =============================================================================
 *
 *   Copyright (c) 2017, Daniel Fernandez (http://github.com/danielfernandez)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 * =============================================================================
 */
package com.github.danielfernandez.matchday.web.controller;


import com.github.danielfernandez.matchday.business.dataviews.MatchInfo;
import com.github.danielfernandez.matchday.business.dataviews.MatchStatus;
import com.github.danielfernandez.matchday.business.entities.MatchComment;
import com.github.danielfernandez.matchday.business.repository.MatchCommentRepository;
import com.github.danielfernandez.matchday.business.repository.MatchInfoRepository;
import com.github.danielfernandez.matchday.business.repository.MatchStatusRepository;
import com.github.danielfernandez.matchday.util.TimestampUtils;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class MatchController {


    private final MatchInfoRepository matchInfoRepository;
    private final MatchStatusRepository matchStatusRepository;
    private final MatchCommentRepository matchCommentRepository;



    public MatchController(
            final MatchInfoRepository matchInfoRepository,
            final MatchStatusRepository matchStatusRepository,
            final MatchCommentRepository matchCommentRepository) {
        super();
        this.matchInfoRepository = matchInfoRepository;
        this.matchStatusRepository = matchStatusRepository;
        this.matchCommentRepository = matchCommentRepository;
    }



    @RequestMapping("/match/{matchId}")
    public String match(@PathVariable String matchId, final Model model) {

        // Get the matchInfo in order to have basic info about the match
        final Mono<MatchInfo> matchInfo = this.matchInfoRepository.getMatchInfo(matchId);

        // This timestamp will allow us to render comments previous to this date server-side, and
        // then create an EventSource at the browser that will retrieve comments after this date
        // as Server-Sent Events (SSE) rendered in JSON
        final String timestamp = TimestampUtils.computeISO8601Timestamp();

        // Get the stream of MatchComment objects
        final Flux<MatchComment> commentStream =
                this.matchCommentRepository
                        .findByMatchIdAndTimestampLessThanEqualOrderByTimestampDesc(matchId, timestamp);

        // Add all attributes to the model
        model.addAttribute("matchId", matchId);                  // Integer
        model.addAttribute("match", matchInfo);                  // Mono, will be resolved before rendering
        model.addAttribute("commentsTimestamp", timestamp);      // String
        model.addAttribute("flux.commentStream", commentStream);  // Flux wrapped in a DataDriver to avoid resolution

        // Return the template name (templates/match.html)
        return "match";

    }




    @RequestMapping(
            value = "/match/{matchId}/statusStream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public String matchStatusStream(@PathVariable String matchId, final Model model) {

        // Get the stream of MatchStatus objects, based on a tailable cursor.
        // See https://docs.mongodb.com/manual/core/tailable-cursors/
        final Flux<MatchStatus> statusStream =
                this.matchStatusRepository.tailMatchStatusStreamByMatchId(matchId);

        // Add the stream as a model attribute
        model.addAttribute("flux.statusStream", statusStream);

        // Return the template name (templates/matchStatus.html)
        return "matchStatus";

    }




    @ResponseBody // The response payload for this request will be rendered in JSON, not HTML
    @RequestMapping(
            value = "/match/{matchId}/commentStream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MatchComment> matchCommentStream(
            @PathVariable String matchId, @RequestParam String timestamp) {

        // Get the stream of MatchComment objects after the timestamp, based on a tailable cursor.
        // See https://docs.mongodb.com/manual/core/tailable-cursors/
        return this.matchCommentRepository.findByMatchIdAndTimestampGreaterThan(matchId, timestamp);

    }

}
