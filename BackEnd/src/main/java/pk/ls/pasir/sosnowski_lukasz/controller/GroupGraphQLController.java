package pk.ls.pasir.sosnowski_lukasz.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import pk.ls.pasir.sosnowski_lukasz.dto.GroupDTO;
import pk.ls.pasir.sosnowski_lukasz.model.Group;
import pk.ls.pasir.sosnowski_lukasz.service.GroupService;

import java.util.List;

@Controller
public class GroupGraphQLController {

    private final GroupService groupService;

    public GroupGraphQLController(GroupService groupService) {
        this.groupService = groupService;
    }

    @QueryMapping
    public List<Group> groups() {
        return groupService.getAllGroups();
    }

    @QueryMapping
    public List<Group> myGroups() {
        return groupService.getMyGroups();
    }

    @MutationMapping
    public Group createGroup(@Argument @Valid GroupDTO groupDTO) {
        return groupService.createGroup(groupDTO);
    }

    @MutationMapping
    public Boolean deleteGroup(@Argument Long id) {
        return groupService.deleteGroup(id);
    }
}