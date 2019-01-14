/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.samples.petclinic.visit.VisitForm;
import org.springframework.samples.petclinic.visit.VisitRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 */
@Controller
class VisitController {

    private final VisitRepository visits;
    private final PetRepository pets;
    @Autowired
    private VetRepository vets;


    @ModelAttribute("vets")
    public Collection<Vet> allVetValues() {
        return this.vets.findAll();
    }


    public VisitController(VisitRepository visits, PetRepository pets) {
        this.visits = visits;
        this.pets = pets;

    }

    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("id");
    }

    /**
     * Called before each and every @RequestMapping annotated method.
     * 2 goals:
     * - Make sure we always have fresh data
     * - Since we do not use the session scope, make sure that Pet object always has an id
     * (Even though id is not part of the form fields)
     *
     * @return Pet
     */


    @ModelAttribute("pet")
    public Pet loadPetWithVisit(@PathVariable("petId") int petId) {
        Pet pet = this.pets.findById(petId);
        return pet;
    }


    @GetMapping("/owners/{ownerId}/pets/{petId}/visits/new")
    public String initNewVisitForm(Map<String, Object> model) {
        VisitForm visitForm = new VisitForm();
        model.put("visitForm", visitForm);
        return "pets/createOrUpdateVisitForm";
    }


    @PostMapping("/owners/{ownerId}/pets/{petId}/visits/new")
    public String processNewVisitForm(@Valid VisitForm visitForm, BindingResult result, Map<String, Object> model) {
        if (result.hasErrors()) {

            return "pets/createOrUpdateVisitForm";
        } else {
            Visit visit = visitFormAdapter(visitForm);
            Pet pet = (Pet) model.get("pet");
            pet.addVisit(visit);
            this.visits.save(visit);
            return "redirect:/owners/{ownerId}";
        }
    }


    @GetMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/edit")
    public String processUpdateVisitForm(@PathVariable("petId") int petId, @PathVariable("visitId") int visitId, Map<String, Object> model) {

        List<Visit> visitList = visits.findByPetId(petId);
        final Visit currentVisit = visitList.stream().filter((visit) ->
            visit.getId() == visitId
        ).findFirst().get();
        VisitForm visitForm = new VisitForm();
        visitForm.setDate(currentVisit.getDate());
        visitForm.setDescription(currentVisit.getDescription());
        visitForm.setVet_id(currentVisit.getVet().getId());
        model.put("visitForm", visitForm);
        return "pets/EditVisitForm";
    }


    @PostMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/edit")
    public String editVisitForm(@Valid VisitForm visitForm, Map<String, Object> model, @PathVariable("visitId") int visitId, BindingResult result) {

        if (result.hasErrors()) {
            return "pets/EditVisitForm";
        } else {
            Visit visit = visitFormAdapter(visitForm);
            visit.setId(visitId);
            Pet pet = (Pet) model.get("pet");
            pet.addVisit(visit);
            visits.save(visit);
            return "redirect:/owners/{ownerId}";
        }
    }


    @GetMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/cancel")
    public String cancelVisitForm(@PathVariable("petId") int petId, @PathVariable("visitId") int visitId) {

        List<Visit> visitList = visits.findByPetId(petId);
        final Visit currentVisit = visitList.stream().filter((visit) ->
            visit.getId() == visitId
        ).findFirst().get();
        currentVisit.setActive(true);

        visits.save(currentVisit);

        return "redirect:/owners/{ownerId}";
    }


    private Visit visitFormAdapter(VisitForm visitForm) {
        Integer vet_id = visitForm.getVet_id();
        Vet vet = vets.findById(vet_id);
        Visit visit = new Visit();
        visit.setVet(vet);
        visit.setDescription(visitForm.getDescription());
        visit.setDate(visitForm.getDate());
        return visit;
    }
}
