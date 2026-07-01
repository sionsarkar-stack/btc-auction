package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.ForbiddenPickEntity;
import com.btc.btc_auction.entity.TribunalVoteEntity;
import com.btc.btc_auction.entity.TrustedCaptainEntity;
import com.btc.btc_auction.model.TribunalCaptainStatus;
import com.btc.btc_auction.model.TribunalVoteRequest;
import com.btc.btc_auction.model.TrustedCaptainRequest;
import com.btc.btc_auction.service.ForbiddenPickService;
import com.btc.btc_auction.service.TribunalService;
import com.btc.btc_auction.service.TribunalVoteService;
import com.btc.btc_auction.service.TrustedCaptainService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = {

                "http://localhost:5173",

                "http://localhost:8080"

})
public class TribunalController {

        private final TrustedCaptainService trustedCaptainService;

        private final TribunalVoteService tribunalVoteService;

        private final TribunalService tribunalService;

        private final ForbiddenPickService forbiddenPickService;

        public TribunalController(

                        TrustedCaptainService trustedCaptainService,

                        TribunalVoteService tribunalVoteService,

                        TribunalService tribunalService,

                        ForbiddenPickService forbiddenPickService) {

                this.trustedCaptainService = trustedCaptainService;

                this.tribunalVoteService = tribunalVoteService;

                this.tribunalService = tribunalService;

                this.forbiddenPickService = forbiddenPickService;
        }

        @GetMapping("/api/tribunal/trusted")
        public List<TrustedCaptainEntity> getTrustedCaptains() {

                return trustedCaptainService.getAll();
        }

        @PostMapping("/api/tribunal/trusted")
        public String saveTrustedCaptain(
                        @RequestBody TrustedCaptainRequest request) {

                trustedCaptainService.deleteByCaptain(
                                request.getCaptainName());

                TrustedCaptainEntity trusted = new TrustedCaptainEntity();

                trusted.setCaptainName(
                                request.getCaptainName());

                trusted.setTrustedCaptain(
                                request.getTrustedCaptain());

                trustedCaptainService.save(
                                trusted);

                return "Trusted Captain Saved";
        }

        @GetMapping("/api/tribunal/votes")
        public List<TribunalVoteEntity> getVotes() {

                return tribunalVoteService.getAll();
        }

        @PostMapping("/api/tribunal/vote")
        public String saveVote(
                        @RequestBody TribunalVoteRequest request) {

                tribunalVoteService.deleteVote(
                                request.getVotingCaptain(),
                                request.getTargetCaptain());

                TribunalVoteEntity vote = new TribunalVoteEntity();

                vote.setVotingCaptain(
                                request.getVotingCaptain());

                vote.setTargetCaptain(
                                request.getTargetCaptain());

                vote.setPlayerName(
                                request.getPlayerName());

                tribunalVoteService.save(
                                vote);

                return "Vote Saved";
        }

        @PostMapping("/api/tribunal/generate")
        public String generateTribunal() {

                tribunalService.generateAll();

                return "Captain Tribunal Generated";
        }

        @GetMapping("/api/tribunal/forbidden")
        public List<ForbiddenPickEntity> getForbiddenPicks() {

                return forbiddenPickService.getAll();
        }

        @GetMapping("/api/tribunal/status")
        public List<TribunalCaptainStatus> getStatus() {

                return tribunalService.getStatus();

        }
}