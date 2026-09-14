package com.btc.btc_auction.controller;

import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.entity.UserEntity;
import com.btc.btc_auction.dto.CaptainConfigDto;
import com.btc.btc_auction.model.CaptainConfigRequest;
import com.btc.btc_auction.service.UserService;
import com.btc.btc_auction.service.PlayerService;
import com.btc.btc_auction.service.TeamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = {

        "http://localhost:5173",

        "http://localhost:8080"

})
public class TeamController {

    private final TeamService teamService;
    private final UserService userService;
    private final PlayerService playerService;

    public TeamController(TeamService teamService, UserService userService, PlayerService playerService) {
        this.teamService = teamService;
        this.userService = userService;
        this.playerService = playerService;
    }

    @GetMapping("/api/teams")
    public List<TeamEntity> getTeams() {
        return teamService.getAllTeams();
    }

    @GetMapping("/api/admin/captains")
    public List<CaptainConfigDto> getCaptains() {
        return teamService.getAllTeams().stream()
                .map(team -> new CaptainConfigDto(team.getCaptainName(), team.getCaptainName(), team.getPurse()))
                .toList();
    }

    @DeleteMapping("/api/admin/captains/{captainName}")
    public String deleteCaptain(@PathVariable String captainName) {
        TeamEntity team = teamService.getTeam(captainName);
        UserEntity user = userService.getUser(captainName);
        if (team == null || user == null || !"CAPTAIN".equalsIgnoreCase(user.getRole())) {
            return "Captain not found.";
        }
        teamService.deleteCaptain(captainName);
        userService.deleteUser(captainName);
        return "Captain deleted.";
    }

    @PostMapping("/api/admin/captains")
    public String configureCaptain(@RequestBody CaptainConfigRequest request) {
        if (request.getCaptainName() == null || request.getCaptainName().isBlank()
                || request.getTotalPoints() < 0) {
            return "Captain name and non-negative total points are required.";
        }

        String currentName = request.getCurrentName() == null ? "" : request.getCurrentName().trim();
        TeamEntity team = currentName.isBlank() ? null : teamService.getTeam(currentName);
        UserEntity user = currentName.isBlank() ? null : userService.getUser(currentName);
        TeamEntity existingTeam = teamService.getTeam(request.getCaptainName());
        if (existingTeam != null && existingTeam != team) {
            return "Captain name already exists.";
        }

        if (team == null) {
            if (userService.getUser(request.getCaptainName()) != null) {
                return "Captain name already exists.";
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                return "Password is required for a new captain.";
            }
            team = teamService.createCaptain(request.getCaptainName().trim(), request.getTotalPoints());
            UserEntity newUser = new UserEntity();
            newUser.setUsername(request.getCaptainName().trim());
            newUser.setPassword(request.getPassword());
            newUser.setRole("CAPTAIN");
            userService.saveUser(newUser);
            return "Captain configuration saved.";
        }
        if (user == null || !"CAPTAIN".equalsIgnoreCase(user.getRole())) {
            return "Captain not found.";
        }

        team.setCaptainName(request.getCaptainName().trim());
        team.setPurse(request.getTotalPoints());
        teamService.saveTeam(team);
        playerService.renameTeamReferences(currentName, request.getCaptainName().trim());
        user.setUsername(request.getCaptainName().trim());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(request.getPassword());
        }
        userService.saveUser(user);
        return "Captain configuration saved.";
    }

}
