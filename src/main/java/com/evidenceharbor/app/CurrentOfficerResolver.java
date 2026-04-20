package com.evidenceharbor.app;

import com.evidenceharbor.domain.Officer;
import com.evidenceharbor.persistence.OfficerRepository;

import java.util.List;

public final class CurrentOfficerResolver {

    private CurrentOfficerResolver() {
    }

    public static Officer resolveDefaultOfficer(OfficerRepository officerRepo) {
        try {
            String osUser = System.getProperty("user.name");
            if (osUser != null) {
                osUser = osUser.trim();
            }

            if (osUser != null && !osUser.isBlank()) {
                Officer byUsername = officerRepo.findByUsername(osUser);
                if (byUsername != null) {
                    return byUsername;
                }

                int slashIdx = osUser.indexOf('\\');
                if (slashIdx >= 0 && slashIdx < osUser.length() - 1) {
                    String noDomain = osUser.substring(slashIdx + 1);
                    byUsername = officerRepo.findByUsername(noDomain);
                    if (byUsername != null) {
                        return byUsername;
                    }
                }

                int atIdx = osUser.indexOf('@');
                if (atIdx > 0) {
                    String localPart = osUser.substring(0, atIdx);
                    byUsername = officerRepo.findByUsername(localPart);
                    if (byUsername != null) {
                        return byUsername;
                    }
                }

                Officer byName = officerRepo.findByName(osUser);
                if (byName != null) {
                    return byName;
                }
            }

            List<Officer> all = officerRepo.findAll();
            if (all.isEmpty()) {
                return null;
            }

            for (Officer officer : all) {
                if ("Active".equalsIgnoreCase(nvl(officer.getStatus()))) {
                    return officer;
                }
            }

            return all.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
