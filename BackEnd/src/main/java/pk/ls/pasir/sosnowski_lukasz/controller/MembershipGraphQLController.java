package pk.ls.pasir.sosnowski_lukasz.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import pk.ls.pasir.sosnowski_lukasz.dto.MembershipDTO;
import pk.ls.pasir.sosnowski_lukasz.dto.MembershipResponseDTO;
import pk.ls.pasir.sosnowski_lukasz.service.MembershipService;

import java.util.List;

@Controller
public class MembershipGraphQLController {

    private final MembershipService membershipService;

    public MembershipGraphQLController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @QueryMapping
    public List<MembershipResponseDTO> groupMembers(@Argument Long groupId) {
        return membershipService.getGroupMembers(groupId);
    }

    @MutationMapping
    public MembershipResponseDTO addMember(@Argument("membershipDTO") @Valid MembershipDTO membershipDTO) {
        return membershipService.addMember(membershipDTO);
    }

    @MutationMapping
    public Boolean removeMember(@Argument Long membershipId) {
        return membershipService.removeMember(membershipId);
    }
}